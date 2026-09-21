package iwish.net;

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
import java.util.List;
import java.util.Map;

public class NetworkClient implements Closeable {

    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final int TIMEOUT = 10_000;

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;

    private int currentUserId = -1;
    private String currentUsername;

    public NetworkClient() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);
        socket.setSoTimeout(TIMEOUT);
        socket.setTcpNoDelay(true);

        in = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8
                )
        );

        out = new BufferedWriter(
                new OutputStreamWriter(
                        socket.getOutputStream(),
                        StandardCharsets.UTF_8
                )
        );
    }

    // ------------------------------------------------------------
    // LOGIN
    // ------------------------------------------------------------

    public Map<String, Object> login(String username, String password)
            throws IOException, Json.JsonException {

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("username", username);
        fields.put("password", password);

        Map<String, Object> response = send(Protocol.LOGIN, fields);

        if (isOk(response)) {
            Object id = response.get("user_id");

            if (id instanceof Number number) {
                currentUserId = number.intValue();
            }

            Object name = response.get("username");

            if (name != null) {
                currentUsername = name.toString();
            }
        }

        return response;
    }

    // ------------------------------------------------------------
    // VIEW CATALOG
    // ------------------------------------------------------------

    public Map<String, Object> viewCatalog()
            throws IOException, Json.JsonException {

        return send(Protocol.VIEW_CATALOG);
    }

    // ------------------------------------------------------------
    // CREATE WISH ITEM
    // ------------------------------------------------------------

    public Map<String, Object> createWishItem(int itemId)
            throws IOException, Json.JsonException {

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("user_id", currentUserId);
        fields.put("item_id", itemId);

        return send(Protocol.CREATE_WISH_ITEM, fields);
    }

    // ------------------------------------------------------------
    // UPDATE WISH ITEM
    // ------------------------------------------------------------

    public Map<String, Object> updateWishItem(int wishId, int itemId)
            throws IOException, Json.JsonException {

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("user_id", currentUserId);
        fields.put("wish_id", wishId);
        fields.put("item_id", itemId);

        return send(Protocol.UPDATE_WISH_ITEM, fields);
    }

    // ------------------------------------------------------------
    // DELETE WISH ITEM
    // ------------------------------------------------------------

    public Map<String, Object> deleteWishItem(int wishId)
            throws IOException, Json.JsonException {

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("user_id", currentUserId);
        fields.put("wish_id", wishId);

        return send(Protocol.DELETE_WISH_ITEM, fields);
    }

    // ------------------------------------------------------------
    // VIEW FRIENDS
    // ------------------------------------------------------------

    public Map<String, Object> viewFriends()
            throws IOException, Json.JsonException {

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("user_id", currentUserId);

        return send(Protocol.VIEW_FRIENDS, fields);
    }

    // ------------------------------------------------------------
    // VIEW FRIEND WISHLIST
    // ------------------------------------------------------------

    public Map<String, Object> viewFriendWishlist(int friendId)
            throws IOException, Json.JsonException {

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("user_id", currentUserId);
        fields.put("friend_id", friendId);

        return send(Protocol.VIEW_FRIEND_WISHLIST, fields);
    }

    // ------------------------------------------------------------
    // GENERIC SEND
    // ------------------------------------------------------------

    public Map<String, Object> send(
            String action,
            Map<String, Object> fields
    ) throws IOException, Json.JsonException {

        Map<String, Object> request = new LinkedHashMap<>();

        request.put(Protocol.ACTION, action);

        if (fields != null) {
            request.putAll(fields);
        }

        String json = Json.write(request);

        out.write(json);
        out.write('\n');
        out.flush();

        String responseLine = in.readLine();

        if (responseLine == null) {
            throw new IOException(
                    "Server closed the connection without replying."
            );
        }

        return Json.parseObject(responseLine);
    }

    public Map<String, Object> send(String action)
            throws IOException, Json.JsonException {

        return send(action, null);
    }

    // ------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------

    public boolean isLoggedIn() {
        return currentUserId != -1;
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public static boolean isOk(Map<String, Object> response) {
        return Protocol.STATUS_OK.equals(
                response.get(Protocol.STATUS)
        );
    }

    public static String getMessage(Map<String, Object> response) {
        Object message = response.get(Protocol.MESSAGE);

        return message == null
                ? ""
                : message.toString();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(
            Map<String, Object> response,
            String key
    ) {
        Object value = response.get(key);

        if (value instanceof List<?>) {
            return (List<Object>) value;
        }

        return List.of();
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
    }
}