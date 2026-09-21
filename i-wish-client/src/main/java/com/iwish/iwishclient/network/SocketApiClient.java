package com.iwish.iwishclient.network;

import com.iwish.iwishclient.model.FriendRequest;
import com.iwish.iwishclient.model.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real networking implementation of {@link ApiClient}.
 *
 * Speaks exactly the transport described in PROTOCOL.md: TCP socket, one
 * JSON object per line out, one JSON object per line back. Drop-in
 * replacement for MockApiClient -- the UI code does not change.
 *
 * Usage (in HelloApplication):
 *     Session.get().setApi(new SocketApiClient("localhost", 5000));
 *
 * Not thread-safe by design, but every call is synchronized so the JavaFX
 * background threads in UiHelper cannot interleave two conversations on the
 * same socket.
 */
public class SocketApiClient implements ApiClient, Closeable {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5000;
    private static final int TIMEOUT_MS = 10_000;

    private final String host;
    private final int port;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public SocketApiClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public SocketApiClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // ---------------- ApiClient ----------------

    @Override
    public User register(String username, String email, String password) throws ApiException {
        Map<String, Object> r = send("REGISTER",
                "username", username, "email", email, "password", password);
        return new User(asInt(r.get("user_id")), asString(r.get("username")));
    }

    @Override
    public User login(String username, String password) throws ApiException {
        Map<String, Object> r = send("LOGIN", "username", username, "password", password);
        return new User(asInt(r.get("user_id")), asString(r.get("username")));
    }

    @Override
    public void addFriend(int userId, String friendUsername) throws ApiException {
        send("ADD_FRIEND", "user_id", userId, "friend_username", friendUsername);
    }

    @Override
    public void acceptFriend(int userId, int requesterId) throws ApiException {
        send("ACCEPT_FRIEND", "user_id", userId, "requester_id", requesterId);
    }

    @Override
    public void declineFriend(int userId, int requesterId) throws ApiException {
        send("DECLINE_FRIEND", "user_id", userId, "requester_id", requesterId);
    }

    @Override
    public void removeFriend(int userId, int friendId) throws ApiException {
        send("REMOVE_FRIEND", "user_id", userId, "friend_id", friendId);
    }

    @Override
    public List<User> viewFriends(int userId) throws ApiException {
        Map<String, Object> r = send("VIEW_FRIENDS", "user_id", userId);
        List<User> friends = new ArrayList<>();
        for (Object o : asList(r.get("friends"))) {
            Map<String, Object> f = asMap(o);
            friends.add(new User(asInt(f.get("user_id")), asString(f.get("username"))));
        }
        return friends;
    }

    @Override
    public List<FriendRequest> viewPendingRequests(int userId) throws ApiException {
        Map<String, Object> r = send("VIEW_PENDING_REQUESTS", "user_id", userId);
        List<FriendRequest> requests = new ArrayList<>();
        for (Object o : asList(r.get("requests"))) {
            Map<String, Object> q = asMap(o);
            requests.add(new FriendRequest(
                    asInt(q.get("requester_id")),
                    asString(q.get("username")),
                    asString(q.get("requested_at"))));
        }
        return requests;
    }

    // ---------------- transport ----------------

    /**
     * Sends one action with the given key/value pairs and returns the fields of
     * a successful response. Throws ApiException on transport failure or on any
     * { "status": "ERROR", "message": ... } reply.
     */
    private synchronized Map<String, Object> send(String action, Object... keyValuePairs) throws ApiException {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("action", action);
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            request.put(String.valueOf(keyValuePairs[i]), keyValuePairs[i + 1]);
        }
        String line = writeJson(request);

        String reply;
        try {
            reply = exchange(line);
        } catch (IOException first) {
            // The server may have dropped an idle socket; try once on a fresh one.
            closeQuietly();
            try {
                reply = exchange(line);
            } catch (IOException second) {
                closeQuietly();
                throw new ApiException("Cannot reach the server at " + host + ":" + port
                        + " (" + second.getMessage() + ")");
            }
        }

        Map<String, Object> response;
        try {
            response = asMap(parseJson(reply));
        } catch (RuntimeException e) {
            throw new ApiException("Malformed response from server");
        }
        if (!"OK".equals(asString(response.get("status")))) {
            String message = asString(response.get("message"));
            throw new ApiException(message == null ? "Request failed" : message);
        }
        return response;
    }

    private String exchange(String line) throws IOException {
        connectIfNeeded();
        out.write(line);
        out.write('\n');
        out.flush();
        String reply = in.readLine();
        if (reply == null) {
            throw new IOException("Server closed the connection without replying");
        }
        return reply;
    }

    private void connectIfNeeded() throws IOException {
        if (socket != null && !socket.isClosed() && socket.isConnected()) {
            return;
        }
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
        socket.setSoTimeout(TIMEOUT_MS);
        socket.setTcpNoDelay(true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    private void closeQuietly() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // nothing useful to do
        }
        socket = null;
        in = null;
        out = null;
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }

    // ---------------- tiny JSON (no external library in this module) ----------------

    private static String writeJson(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(value, sb);
        return sb.toString();
    }

    private static void writeValue(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(s, sb);
        } else if (value instanceof Boolean b) {
            sb.append(b);
        } else if (value instanceof Integer || value instanceof Long) {
            sb.append(value);
        } else if (value instanceof Number n) {
            sb.append(n.doubleValue());
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean firstEntry = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!firstEntry) sb.append(',');
                firstEntry = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                writeValue(e.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof List<?> list) {
            sb.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                writeValue(list.get(i), sb);
            }
            sb.append(']');
        } else {
            writeString(String.valueOf(value), sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static Object parseJson(String text) {
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object value = p.readValue();
        p.skipWhitespace();
        if (!p.atEnd()) {
            throw new IllegalArgumentException("Trailing characters after JSON value");
        }
        return value;
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) { this.s = s; }

        boolean atEnd() { return i >= s.length(); }

        void skipWhitespace() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        Object readValue() {
            skipWhitespace();
            if (atEnd()) throw new IllegalArgumentException("Unexpected end of JSON");
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't', 'f' -> readBoolean();
                case 'n' -> readNull();
                default -> readNumber();
            };
        }

        Map<String, Object> readObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') { i++; return map; }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                map.put(key, readValue());
                skipWhitespace();
                char c = next();
                if (c == '}') return map;
                if (c != ',') throw new IllegalArgumentException("Expected , or } in object");
            }
        }

        List<Object> readArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') { i++; return list; }
            while (true) {
                list.add(readValue());
                skipWhitespace();
                char c = next();
                if (c == ']') return list;
                if (c != ',') throw new IllegalArgumentException("Expected , or ] in array");
            }
        }

        String readString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                char esc = next();
                switch (esc) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/'  -> sb.append('/');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    case 'b'  -> sb.append('\b');
                    case 'f'  -> sb.append('\f');
                    case 'u'  -> {
                        if (i + 4 > s.length()) throw new IllegalArgumentException("Bad \\u escape");
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw new IllegalArgumentException("Bad escape: \\" + esc);
                }
            }
        }

        Boolean readBoolean() {
            if (s.startsWith("true", i))  { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            throw new IllegalArgumentException("Invalid literal");
        }

        Object readNull() {
            if (s.startsWith("null", i)) { i += 4; return null; }
            throw new IllegalArgumentException("Invalid literal");
        }

        Number readNumber() {
            int start = i;
            if (peek() == '-' || peek() == '+') i++;
            boolean fractional = false;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isDigit(c)) { i++; }
                else if (c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') { fractional = true; i++; }
                else break;
            }
            String token = s.substring(start, i);
            if (token.isEmpty()) throw new IllegalArgumentException("Invalid number");
            if (fractional) return Double.parseDouble(token);
            return Long.parseLong(token);
        }

        char peek() {
            if (atEnd()) throw new IllegalArgumentException("Unexpected end of JSON");
            return s.charAt(i);
        }

        char next() {
            if (atEnd()) throw new IllegalArgumentException("Unexpected end of JSON");
            return s.charAt(i++);
        }

        void expect(char c) {
            if (next() != c) throw new IllegalArgumentException("Expected " + c);
        }
    }

    // ---------------- field helpers ----------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map) return (Map<String, Object>) value;
        throw new IllegalArgumentException("Expected a JSON object");
    }

    private static List<Object> asList(Object value) {
        if (value instanceof List<?> list) return new ArrayList<>(list);
        return new ArrayList<>(); // missing or null list -> empty, so the UI shows "nothing here"
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int asInt(Object value) throws ApiException {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        throw new ApiException("Server response is missing a numeric id");
    }
}