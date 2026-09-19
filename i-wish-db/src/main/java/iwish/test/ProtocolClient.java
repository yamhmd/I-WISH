package iwish.test;

import iwish.net.Json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A tiny client that speaks the protocol: one JSON object out, one JSON
 * object back.
 *
 * Written for ProtocolVerifier, but Persons 4, 5 and 6 can use the same class
 * from their GUI code -- send("LOGIN", Map.of("username", u, "password", p))
 * is the whole networking layer they need.
 *
 * Note: it is NOT thread-safe on purpose. One socket, one conversation. For
 * several simultaneous clients, create several instances (that is exactly
 * what the concurrency check does).
 */
public class ProtocolClient implements Closeable {

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;

    public ProtocolClient(String host, int port, int timeoutMs) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);
        socket.setTcpNoDelay(true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public ProtocolClient(String host, int port) throws IOException {
        this(host, port, 10_000);
    }

    /** Sends an action with the given fields and returns the parsed response. */
    public Map<String, Object> send(String action, Map<String, Object> fields) throws IOException, Json.JsonException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("action", action);
        if (fields != null) {
            request.putAll(fields);
        }
        return Json.parseObject(sendRaw(Json.write(request)));
    }

    public Map<String, Object> send(String action) throws IOException, Json.JsonException {
        return send(action, null);
    }

    /** Sends a line exactly as given -- used to iwish.test malformed input. Returns the raw reply line. */
    public String sendRaw(String line) throws IOException {
        out.write(line);
        out.write('\n');
        out.flush();
        String reply = in.readLine();
        if (reply == null) {
            throw new IOException("Server closed the connection without replying");
        }
        return reply;
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // nothing useful to do
        }
    }

    // ---- small helpers so tests read cleanly ----

    public static Map<String, Object> fields(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Expected key/value pairs");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put(String.valueOf(keyValuePairs[i]), keyValuePairs[i + 1]);
        }
        return map;
    }
}
