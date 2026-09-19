package iwish.server;

import iwish.net.Json;
import iwish.net.Protocol;
import iwish.net.Request;
import iwish.net.Response;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * One connected client, handled on one thread (Person 2, task 4).
 *
 * The loop is exactly the transport rule from PROTOCOL.md: "one message per
 * request/response, JSON-encoded, newline-terminated". Read a line, answer
 * with a line, repeat until the client hangs up.
 *
 * Nothing a client sends is allowed to kill this thread or the iwish.server
 * (Person 2, task 11). Garbage JSON, a missing action, an unknown action, a
 * field of the wrong type, an exception thrown deep inside business logic --
 * all of them come back as the standard
 *     { "status": "ERROR", "message": "..." }
 * and the connection stays usable.
 *
 * Shutdown (task 10): the socket is closed in a finally block whatever
 * happens, and the handler deregisters itself from the iwish.server's connection
 * set so no socket or thread is left behind.
 */
public class ClientHandler implements Runnable {

    /** Drop a connection that sends nothing at all for this long, so dead sockets can't accumulate. */
    private static final int IDLE_TIMEOUT_MS = 10 * 60 * 1000;

    /** Refuse absurdly long lines instead of buffering them into an OutOfMemoryError. */
    private static final int MAX_LINE_LENGTH = 64 * 1024;

    private final Socket socket;
    private final Dispatcher dispatcher;
    private final IWishServer server;
    private final Session session;

    public ClientHandler(Socket socket, Dispatcher dispatcher, IWishServer server, String connectionId) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.server = server;
        this.session = new Session(connectionId);
    }

    public Session session() {
        return session;
    }

    public void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // already closed -- nothing useful to do
        }
    }

    @Override
    public void run() {
        ServerLog.info("Connected: " + session + " from " + socket.getRemoteSocketAddress()
            + " (open connections: " + server.openConnectionCount() + ")");

        try (BufferedReader in = new BufferedReader(
                 new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(IDLE_TIMEOUT_MS);
            socket.setTcpNoDelay(true); // request/response traffic -- don't wait to batch

            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) {
                    continue; // tolerate stray newlines from a client
                }
                if (line.length() > MAX_LINE_LENGTH) {
                    send(out, Response.error("Request too large"));
                    continue;
                }
                Response response = handleLine(line);
                send(out, response);
            }
            ServerLog.info("Client closed the connection: " + session);

        } catch (SocketTimeoutException e) {
            ServerLog.warn("Idle timeout, dropping " + session);
        } catch (SocketException e) {
            // Normal when a client is killed mid-connection -- not an error.
            ServerLog.info("Connection reset: " + session + " (" + e.getMessage() + ")");
        } catch (IOException e) {
            ServerLog.warn("I/O error on " + session + ": " + e.getMessage());
        } finally {
            closeQuietly();
            server.connectionClosed(this);
            ServerLog.info("Disconnected: " + session
                + " (open connections: " + server.openConnectionCount() + ")");
        }
    }

    /** Turns one raw line into the response that should go back. Never throws. */
    Response handleLine(String line) {
        Map<String, Object> fields;
        try {
            fields = Json.parseObject(line);
        } catch (Json.JsonException e) {
            ServerLog.warn("Malformed JSON from " + session + ": " + e.getMessage());
            return Response.error(Protocol.Errors.MALFORMED_JSON);
        } catch (RuntimeException e) {
            ServerLog.warn("Unparseable request from " + session + ": " + e);
            return Response.error(Protocol.Errors.MALFORMED_JSON);
        }

        Request request = new Request(fields);
        String action = request.action();
        ServerLog.info(session + " -> " + (action == null ? "(no action)" : action));

        Response response = dispatcher.dispatch(request, session);
        dispatcher.bindSessionIfAuthenticated(action, response, session);

        if (!response.isOk()) {
            ServerLog.info(session + " <- ERROR " + response.get(Protocol.MESSAGE));
        }
        return response;
    }

    private void send(BufferedWriter out, Response response) throws IOException {
        out.write(response.toJson());
        out.write('\n');           // newline-terminated, per PROTOCOL.md
        out.flush();               // one message per request -- never leave it in the buffer
    }
}
