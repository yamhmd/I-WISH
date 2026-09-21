package iwish.logic;

import iwish.net.Protocol;
import iwish.net.Request;
import iwish.net.Response;
import iwish.server.Session;
import iwish.util.PasswordUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TASK_ASSIGNMENTS, Person 2 item 6: "Stub every action from PROTOCOL.md with
 * a hardcoded response, just to prove round-trip works end to end", and item 7:
 * hand this to Persons 4, 5 and 6 so the client screens can be wired to real
 * network calls before any business logic exists.
 *
 * Every response here is shaped exactly like the example in PROTOCOL.md --
 * same field names, same types -- so a client written against the stub keeps
 * working unchanged when Person 3's real logic is swapped in.
 *
 * NOTHING HERE TOUCHES THE DATABASE. The only state is a handful of in-memory
 * maps, so that REGISTER/LOGIN hand out distinct user_ids and the session
 * check behaves realistically during multi-client testing. Everything else is
 * canned data.
 *
 * REGISTER and LOGIN do run the real PasswordUtil (Person 2 item 9): the
 * plaintext from the client is hashed here at the edge and only the hash is
 * kept, which is the same place Person 3's real implementation must hash
 * before calling UserDAO.createUser().
 */
public class StubBusinessLogic implements BusinessLogic {

    private final AtomicInteger nextUserId = new AtomicInteger(1);
    private final Map<String, Integer> userIds = new ConcurrentHashMap<>();
    private final Map<String, String> passwordHashes = new ConcurrentHashMap<>();

    @Override
    public Response register(Request request, Session session) throws Exception {
        String username = request.getString("username");
        request.getString("email"); // validated for shape; the stub doesn't store it
        String password = request.getString("password");

        if (userIds.containsKey(username)) {
            return Response.error(Protocol.Errors.USERNAME_TAKEN);
        }

        // Hash at the edge -- the plaintext never goes any further than this line.
        String hash = PasswordUtil.hash(password);

        int userId = nextUserId.getAndIncrement();
        userIds.put(username, userId);
        passwordHashes.put(username, hash);

        session.login(userId, username);
        return Response.ok()
            .with("user_id", userId)
            .with("username", username);
    }

    @Override
    public Response login(Request request, Session session) throws Exception {
        String username = request.getString("username");
        String password = request.getString("password");

        String hash = passwordHashes.get(username);
        // Stub convenience: any not-yet-registered username logs in, so client
        // developers aren't forced to register first. Real logic must return
        // "Invalid credentials" here instead.
        if (hash != null && !PasswordUtil.verify(password, hash)) {
            return Response.error(Protocol.Errors.INVALID_CREDENTIALS);
        }
        int userId = userIds.computeIfAbsent(username, u -> nextUserId.getAndIncrement());

        session.login(userId, username);
        return Response.ok()
            .with("user_id", userId)
            .with("username", username);
    }

    @Override
    public Response addFriend(Request request, Session session) throws Exception {
        request.getInt("user_id");
        String friendUsername = request.getString("friend_username");
        if (friendUsername.equals(session.username())) {
            return Response.error("Cannot add yourself as a friend");
        }
        return Response.ok("Friend request sent");
    }

    @Override
    public Response acceptFriend(Request request, Session session) throws Exception {
        request.getInt("user_id");
        request.getInt("requester_id");
        return Response.ok("Friend request accepted");
    }

    @Override
    public Response declineFriend(Request request, Session session) throws Exception {
        request.getInt("user_id");
        request.getInt("requester_id");
        return Response.ok("Friend request declined");
    }

    @Override
    public Response removeFriend(Request request, Session session) throws Exception {
        request.getInt("user_id");
        request.getInt("friend_id");
        return Response.ok("Friend removed");
    }

    @Override
    public Response viewFriends(Request request, Session session) throws Exception {
        request.getInt("user_id");
        List<Object> friends = new ArrayList<>();
        friends.add(entry("user_id", 2, "username", "mohamed"));
        friends.add(entry("user_id", 3, "username", "sara"));
        return Response.ok().with("friends", friends);
    }

    @Override
    public Response viewPendingRequests(Request request, Session session) throws Exception {
        request.getInt("user_id");
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("requester_id", 1);
        req.put("username", "ahmed");
        req.put("requested_at", "2026-09-17T10:00:00Z");
        return Response.ok().with("requests", List.of(req));
    }

    @Override
    public Response viewCatalog(Request request, Session session) {
        List<Object> items = new ArrayList<>();
        items.add(catalogItem(10, "Headphones", "500.00"));
        items.add(catalogItem(11, "Watch", "1200.00"));
        return Response.ok().with("items", items);
    }

    @Override
    public Response createWishItem(Request request, Session session) throws Exception {
        request.getInt("user_id");
        int itemId = request.getInt("item_id");
        return Response.ok()
            .with("wish_id", 55)
            .with("item_id", itemId)
            .with("amount_raised", new BigDecimal("0.00"))
            .with("is_complete", false);
    }

    @Override
    public Response updateWishItem(Request request, Session session) throws Exception {
        request.getInt("user_id");
        request.getInt("wish_id");
        request.getInt("item_id");
        return Response.ok("Wish item updated");
    }

    @Override
    public Response deleteWishItem(Request request, Session session) throws Exception {
        request.getInt("user_id");
        int wishId = request.getInt("wish_id");
        // Canned edge case so Person 5 can build the error UI (their item 6):
        // wish_id 99 always pretends to have contributions.
        if (wishId == 99) {
            return Response.error(Protocol.Errors.CANNOT_DELETE);
        }
        return Response.ok("Wish item deleted");
    }

    @Override
    public Response viewMyWishlist(Request request, Session session) throws Exception {
        request.getInt("user_id");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("wish_id", 55);
        item.put("item_id", 10);
        item.put("name", "Headphones");
        item.put("price", new BigDecimal("500.00"));
        item.put("amount_raised", new BigDecimal("0.00"));
        item.put("is_complete", false);
        return Response.ok().with("wish_items", List.of(item));
    }

    @Override
    public Response viewFriendWishlist(Request request, Session session) throws Exception {
        request.getInt("user_id");
        int friendId = request.getInt("friend_id");
        // Canned edge case for Person 5: friend_id 999 is not a friend.
        if (friendId == 999) {
            return Response.error(Protocol.Errors.NOT_FRIENDS);
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("wish_id", 60);
        item.put("item_id", 10);
        item.put("name", "Headphones");
        item.put("price", new BigDecimal("500.00"));
        item.put("amount_raised", new BigDecimal("200.00"));
        item.put("is_complete", false);
        return Response.ok()
            .with("friend_username", "mohamed")
            .with("wish_items", List.of(item));
    }

    @Override
    public Response contribute(Request request, Session session) throws Exception {
        request.getInt("user_id");
        request.getInt("wish_id");
        BigDecimal amount = request.getAmount("amount");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.error("Amount must be greater than zero");
        }
        // Canned edge case for Person 6 (their item 4): anything over 300
        // "exceeds the remaining price" on the canned 500/200 item.
        if (amount.compareTo(new BigDecimal("300.00")) > 0) {
            return Response.error(Protocol.Errors.AMOUNT_EXCEEDS);
        }
        boolean completed = amount.compareTo(new BigDecimal("300.00")) == 0;
        return Response.ok("Contribution recorded").with("wish_completed", completed);
    }

    @Override
    public Response getNotifications(Request request, Session session) throws Exception {
        request.getInt("user_id");
        List<Object> notifications = new ArrayList<>();
        notifications.add(notification(200, "receiver", "Your item 'Headphones' was fully funded!"));
        notifications.add(notification(201, "buyer", "Your contribution to 'Headphones' helped complete it!"));
        return Response.ok().with("notifications", notifications);
    }

    @Override
    public Response markNotificationRead(Request request, Session session) throws Exception {
        request.getInt("user_id");
        request.getInt("notif_id");
        return Response.ok("Notification marked as read");
    }

    // ------------------------------------------------------------------

    private static Map<String, Object> entry(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private static Map<String, Object> catalogItem(int id, String name, String price) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("item_id", id);
        m.put("name", name);
        m.put("price", new BigDecimal(price));
        return m;
    }

    private static Map<String, Object> notification(int id, String type, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("notif_id", id);
        m.put("type", type);
        m.put("message", message);
        m.put("is_read", false);
        m.put("created_at", "2026-09-17T15:00:00Z");
        return m;
    }
}
