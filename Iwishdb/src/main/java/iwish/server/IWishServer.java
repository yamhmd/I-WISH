package iwish.server;

import iwish.logic.BusinessLogic;
import iwish.logic.RealBusinessLogic;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The i-Wish iwish.server (Person 2, tasks 3, 4 and 10).
 *
 *   task 3  -- open a socket and accept incoming connections
 *   task 4  -- one thread per connected client, so several users are served
 *             at the same time
 *   task 10 -- clean disconnects: every accepted socket is tracked, every
 *             finished handler deregisters itself, and shutdown closes the
 *             listener, all live sockets and the pool. No leaked threads.
 *
 * Run it:
 *
 *     java -cp out iwish.server.IWishServer                 # real logic, port 5000
 *     java -cp out iwish.server.IWishServer 5050            # real logic, port 5050
 *     java -Diwish.logic=iwish.logic.StubBusinessLogic -cp out iwish.server.IWishServer
 *
 * Defaults to RealBusinessLogic (Person 3 wired in). Pass StubBusinessLogic
 * via -Diwish.logic when you only need canned round-trip responses without MySQL.
 */
public class IWishServer {

    public static final int DEFAULT_PORT = 5000;

    private final int port;
    private final Dispatcher dispatcher;

    private final Set<ClientHandler> openConnections = ConcurrentHashMap.newKeySet();
    private final AtomicInteger connectionCounter = new AtomicInteger();

    private ServerSocket serverSocket;
    private ExecutorService clientPool;
    private volatile boolean running;

    public IWishServer(int port, BusinessLogic logic) {
        this.port = port;
        this.dispatcher = new Dispatcher(logic);
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /** Binds the port and blocks in the accept loop until stop() is called. */
    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));

        clientPool = Executors.newCachedThreadPool(namedThreads());
        running = true;

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "iwish-shutdown"));

        ServerLog.info("i-Wish iwish.server listening on port " + actualPort());
        ServerLog.info("Business logic: " + dispatcher.logic().getClass().getName());
        ServerLog.info("Actions served (" + dispatcher.supportedActions().size() + "): "
            + String.join(", ", dispatcher.supportedActions()));

        acceptLoop();
    }

    private void acceptLoop() {
        while (running) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                if (running) {
                    ServerLog.error("accept() failed: " + e.getMessage());
                    continue;      // a single bad accept must not end the iwish.server
                }
                break;             // stop() closed the listener -- expected
            }

            String connectionId = "conn-" + connectionCounter.incrementAndGet();
            ClientHandler handler = new ClientHandler(socket, dispatcher, this, connectionId);
            openConnections.add(handler);

            try {
                clientPool.execute(handler);
            } catch (RejectedExecutionException e) {
                // Happens only during shutdown; don't leave the socket open.
                openConnections.remove(handler);
                handler.closeQuietly();
            }
        }
        ServerLog.info("Accept loop finished.");
    }

    /** Closes the listener, every live client socket, and the thread pool. Safe to call twice. */
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        ServerLog.info("Shutting down -- closing " + openConnections.size() + " open connection(s)...");

        try {
            if (serverSocket != null) {
                serverSocket.close();   // wakes the blocked accept()
            }
        } catch (IOException e) {
            ServerLog.warn("Error closing the listening socket: " + e.getMessage());
        }

        for (ClientHandler handler : openConnections) {
            handler.closeQuietly();     // wakes each blocked readLine()
        }

        if (clientPool != null) {
            clientPool.shutdown();
            try {
                if (!clientPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    ServerLog.warn("Some client threads did not finish in time -- forcing shutdown.");
                    clientPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                clientPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        ServerLog.info("Shutdown complete. Leftover tracked connections: " + openConnections.size());
    }

    /** Called by a handler when its socket is finished, so nothing is tracked forever. */
    void connectionClosed(ClientHandler handler) {
        openConnections.remove(handler);
    }

    public int openConnectionCount() {
        return openConnections.size();
    }

    /** The port actually bound -- useful when the iwish.server was started on port 0 in a iwish.test. */
    public int actualPort() {
        return serverSocket == null ? port : serverSocket.getLocalPort();
    }

    public boolean isRunning() {
        return running;
    }

    private static ThreadFactory namedThreads() {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, "iwish-client-" + n.incrementAndGet());
            t.setDaemon(false);
            return t;
        };
    }

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: java iwish.iwish.server.IWishServer [port]");
                System.exit(1);
            }
        }
        new IWishServer(port, loadLogic()).start();
    }

    /**
     * Picks the business logic implementation. Defaults to {@link RealBusinessLogic};
     * set -Diwish.logic=&lt;fully.qualified.Class&gt; to override (e.g. StubBusinessLogic).
     */
    private static BusinessLogic loadLogic() {
        String className = System.getProperty("iwish.logic");
        if (className == null || className.isBlank()) {
            ServerLog.info("Running with REAL business logic (database-backed).");
            return new RealBusinessLogic();
        }
        try {
            Object instance = Class.forName(className).getDeclaredConstructor().newInstance();
            if (!(instance instanceof BusinessLogic logic)) {
                throw new IllegalStateException(className + " does not implement iwish.logic.BusinessLogic");
            }
            ServerLog.info("Running with business logic: " + className);
            return logic;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not load business logic class: " + className, e);
        }
    }
}
