package iwish.logic;

import iwish.net.Protocol;
import iwish.net.Request;
import iwish.net.Response;
import iwish.server.Session;
import iwish.service.ContributionService;
import iwish.service.FriendService;
import iwish.service.NotificationService;
import iwish.service.UserService;
import iwish.service.WishlistService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * TASK_ASSIGNMENTS, Person 3: "the logic behind every protocol action,
 * connecting Person 1's queries to Person 2's dispatch layer."
 *
 * This class was the missing piece — IWishServer.loadLogic() imports and
 * instantiates it by default, but it didn't exist anywhere in the project,
 * so the server could not compile except via the -Diwish.logic override that
 * falls back to StubBusinessLogic's canned data.
 *
 * All the real work was already done in iwish.service.* (UserService,
 * FriendService, WishlistService, ContributionService, NotificationService),
 * which already validate business rules and throw IllegalStateException with
 * the exact PROTOCOL.md wording. This class is the one-method-per-action seam
 * described in BusinessLogic: read the request's own fields, call the
 * matching service method, and shape the result as Response.ok(...). Nothing
 * here needs its own try/catch — Dispatcher already turns
 * IllegalStateException and SQLException into the correct ERROR responses.
 */
public class RealBusinessLogic implements BusinessLogic {

    private final UserService userService = new UserService();
    private final FriendService friendService = new FriendService();
    private final WishlistService wishlistService = new WishlistService();
    private final ContributionService contributionService = new ContributionService();
    private final NotificationService notificationService = new NotificationService();

    // REGISTER — PROTOCOL.md #1
    @Override
    public Response register(Request request, Session session) throws Exception {
        String username = request.getString("username");
        String email = request.getString("email");
        String password = request.getString("password");

        int userId = userService.register(username, email, password);

        // Dispatcher.bindSessionIfAuthenticated() reads user_id/username off
        // this response and calls session.login(...) itself once it sees the
        // response is OK, so this class doesn't touch Session directly.
        return Response.ok()
                .with("user_id", userId)
                .with("username", username.trim());
    }

    // LOGIN — #2
    @Override
    public Response login(Request request, Session session) throws Exception {
        String username = request.getString("username");
        String password = request.getString("password");

        Map<String, Object> result = userService.login(username, password);
        return Response.ok()
                .with("user_id", result.get("user_id"))
                .with("username", result.get("username"));
    }

    // ADD_FRIEND — #3
    @Override
    public Response addFriend(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        String friendUsername = request.getString("friend_username");
        friendService.addFriend(userId, friendUsername);
        return Response.ok("Friend request sent");
    }

    // ACCEPT_FRIEND — #4
    @Override
    public Response acceptFriend(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        int requesterId = request.getInt("requester_id");
        friendService.acceptFriend(userId, requesterId);
        return Response.ok("Friend request accepted");
    }

    // DECLINE_FRIEND — #5
    @Override
    public Response declineFriend(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        int requesterId = request.getInt("requester_id");
        friendService.declineFriend(userId, requesterId);
        return Response.ok("Friend request declined");
    }

    // REMOVE_FRIEND — #6
    @Override
    public Response removeFriend(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        int friendId = request.getInt("friend_id");
        friendService.removeFriend(userId, friendId);
        return Response.ok("Friend removed");
    }

    // VIEW_FRIENDS — #7
    @Override
    public Response viewFriends(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        List<Map<String, Object>> friends = friendService.viewFriends(userId);
        return Response.ok().with("friends", friends);
    }

    // VIEW_PENDING_REQUESTS — #8
    @Override
    public Response viewPendingRequests(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        List<Map<String, Object>> requests = friendService.viewPendingRequests(userId);
        return Response.ok().with("requests", requests);
    }

    // VIEW_CATALOG — #9 (public action, no user_id on the request)
    @Override
    public Response viewCatalog(Request request, Session session) throws Exception {
        List<Map<String, Object>> items = wishlistService.viewCatalog();
        return Response.ok().with("items", items);
    }

    // CREATE_WISH_ITEM — #10
    @Override
    public Response createWishItem(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        int itemId = request.getInt("item_id");
        Map<String, Object> wish = wishlistService.createWishItem(userId, itemId);
        return Response.ok()
                .with("wish_id", wish.get("wish_id"))
                .with("item_id", wish.get("item_id"))
                .with("amount_raised", wish.get("amount_raised"))
                .with("is_complete", wish.get("is_complete"));
    }

    // UPDATE_WISH_ITEM — #11
    @Override
    public Response updateWishItem(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        int wishId = request.getInt("wish_id");
        int itemId = request.getInt("item_id");
        wishlistService.updateWishItem(userId, wishId, itemId);
        return Response.ok("Wish item updated");
    }

    // DELETE_WISH_ITEM — #12
    @Override
    public Response deleteWishItem(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        int wishId = request.getInt("wish_id");
        wishlistService.deleteWishItem(userId, wishId);
        return Response.ok("Wish item deleted");
    }

    // VIEW_MY_WISHLIST — #13
    @Override
    public Response viewMyWishlist(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        Map<String, Object> result = wishlistService.viewMyWishlist(userId);
        return Response.ok().with("wish_items", result.get("wish_items"));
    }

    // VIEW_FRIEND_WISHLIST — #14
    @Override
    public Response viewFriendWishlist(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        int friendId = request.getInt("friend_id");
        Map<String, Object> result = wishlistService.viewFriendWishlist(userId, friendId);
        return Response.ok()
                .with("friend_username", result.get("friend_username"))
                .with("wish_items", result.get("wish_items"));
    }

    // CONTRIBUTE — #15
    @Override
    public Response contribute(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        int wishId = request.getInt("wish_id");
        BigDecimal amount = request.getAmount("amount");

        Map<String, Object> result = contributionService.contribute(userId, wishId, amount);
        return Response.ok("Contribution recorded")
                .with("wish_completed", result.get("wish_completed"));
    }

    // GET_NOTIFICATIONS — #16
    @Override
    public Response getNotifications(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        List<Map<String, Object>> notifications = notificationService.getNotifications(userId);
        return Response.ok().with("notifications", notifications);
    }

    // MARK_NOTIFICATION_READ — #17
    @Override
    public Response markNotificationRead(Request request, Session session) throws Exception {
        int userId = request.getInt("user_id");
        int notifId = request.getInt("notif_id");
        notificationService.markNotificationRead(userId, notifId);
        return Response.ok("Notification marked as read");
    }
}
