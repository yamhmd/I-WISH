package iwish.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Full end-to-end iwish.test of the business-logic layer, covering all 16
 * actions across the 5 Services. Run this directly (right-click -> Run)
 * AFTER MySQL is running and schema.sql + seed.sql have been executed.
 */
public class ServiceTestMain {

    public static void main(String[] args) throws Exception {
        UserService userService = new UserService();
        FriendService friendService = new FriendService();
        WishlistService wishlistService = new WishlistService();
        ContributionService contributionService = new ContributionService();
        NotificationService notificationService = new NotificationService();

        System.out.println("=== 1. REGISTER ===");
        int ahmedId = userService.register("ahmed_t", "ahmed_t@mail.com", "pass123");
        int mohamedId = userService.register("mohamed_t", "mohamed_t@mail.com", "pass123");
        int saraId = userService.register("sara_t", "sara_t@mail.com", "pass123");
        System.out.println("ahmed=" + ahmedId + " mohamed=" + mohamedId + " sara=" + saraId);

        System.out.println("\n=== 2. LOGIN (correct + wrong password) ===");
        Map<String, Object> loginOk = userService.login("ahmed_t", "pass123");
        System.out.println("Correct login: " + loginOk);
        try {
            userService.login("ahmed_t", "wrongpass");
            System.out.println("ERROR: should have thrown!");
        } catch (IllegalStateException e) {
            System.out.println("Wrong password correctly rejected: " + e.getMessage());
        }

        System.out.println("\n=== 3. ADD_FRIEND: mohamed -> ahmed ===");
        friendService.addFriend(mohamedId, "ahmed_t");
        System.out.println("Request sent");

        System.out.println("\n=== 8. VIEW_PENDING_REQUESTS (ahmed's inbox) ===");
        List<Map<String, Object>> pending = friendService.viewPendingRequests(ahmedId);
        System.out.println(pending);

        System.out.println("\n=== 4. ACCEPT_FRIEND: ahmed accepts mohamed ===");
        friendService.acceptFriend(ahmedId, mohamedId);
        System.out.println("Accepted");

        System.out.println("\n=== 5. DECLINE_FRIEND: sara -> ahmed, ahmed declines ===");
        friendService.addFriend(saraId, "ahmed_t");
        friendService.declineFriend(ahmedId, saraId);
        System.out.println("Declined (sara and ahmed are NOT friends)");

        // We need sara to actually be friends with ahmed for later CONTRIBUTE
        // iwish.test, so send + accept again properly.
        friendService.addFriend(saraId, "ahmed_t");
        friendService.acceptFriend(ahmedId, saraId);

        System.out.println("\n=== 7. VIEW_FRIENDS (ahmed's list) ===");
        List<Map<String, Object>> friends = friendService.viewFriends(ahmedId);
        System.out.println(friends);

        System.out.println("\n=== 9. VIEW_CATALOG ===");
        List<Map<String, Object>> catalog = wishlistService.viewCatalog();
        System.out.println(catalog);
        int headphonesItemId = (int) catalog.stream()
                .filter(i -> "Headphones".equals(i.get("name")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Run seed.sql first — 'Headphones' not found"))
                .get("item_id");

        System.out.println("\n=== 10. CREATE_WISH_ITEM: ahmed adds Headphones ===");
        Map<String, Object> wishItem = wishlistService.createWishItem(ahmedId, headphonesItemId);
        System.out.println(wishItem);
        int wishId = (int) wishItem.get("wish_id");

        System.out.println("\n=== 13. VIEW_FRIEND_WISHLIST: mohamed views ahmed's list ===");
        Map<String, Object> friendWishlist = wishlistService.viewFriendWishlist(mohamedId, ahmedId);
        System.out.println(friendWishlist);

        System.out.println("\n=== 13b. VIEW_FRIEND_WISHLIST: should fail for non-friends ===");
        int strangerId = userService.register("stranger_t", "stranger_t@mail.com", "pass123");
        try {
            wishlistService.viewFriendWishlist(strangerId, ahmedId);
            System.out.println("ERROR: should have thrown!");
        } catch (IllegalStateException e) {
            System.out.println("Correctly rejected: " + e.getMessage());
        }

        System.out.println("\n=== 14. CONTRIBUTE: mohamed 200, sara 300 (should complete it) ===");
        Map<String, Object> c1 = contributionService.contribute(mohamedId, wishId, new BigDecimal("200.00"));
        System.out.println("Mohamed's contribution: " + c1);
        Map<String, Object> c2 = contributionService.contribute(saraId, wishId, new BigDecimal("300.00"));
        System.out.println("Sara's contribution: " + c2);

        System.out.println("\n=== 14b. CONTRIBUTE: over-contribute after completion (should fail) ===");
        try {
            contributionService.contribute(saraId, wishId, new BigDecimal("50.00"));
            System.out.println("ERROR: should have thrown!");
        } catch (IllegalStateException e) {
            System.out.println("Correctly rejected: " + e.getMessage());
        }

        System.out.println("\n=== 14c. CONTRIBUTE: cannot contribute to own item ===");
        int wishId2 = (int) wishlistService.createWishItem(ahmedId,
                (int) catalog.stream().filter(i -> "Smart Watch".equals(i.get("name"))).findFirst().get().get("item_id")
        ).get("wish_id");
        try {
            contributionService.contribute(ahmedId, wishId2, new BigDecimal("100.00"));
            System.out.println("ERROR: should have thrown!");
        } catch (IllegalStateException e) {
            System.out.println("Correctly rejected: " + e.getMessage());
        }


        System.out.println("\n=== 15. GET_NOTIFICATIONS ===");
        System.out.println("Ahmed (receiver): " + notificationService.getNotifications(ahmedId));
        System.out.println("Mohamed (buyer): " + notificationService.getNotifications(mohamedId));
        System.out.println("Sara (buyer): " + notificationService.getNotifications(saraId));

        System.out.println("\n=== 16. MARK_NOTIFICATION_READ ===");
        List<Map<String, Object>> ahmedNotifs = notificationService.getNotifications(ahmedId);
        int firstNotifId = (int) ahmedNotifs.get(0).get("notif_id");
        notificationService.markNotificationRead(ahmedId, firstNotifId);
        System.out.println("Marked notif " + firstNotifId + " as read");

        System.out.println("\n=== 12. DELETE_WISH_ITEM: ahmed deletes wishId2 (no contributions, should work) ===");
        wishlistService.deleteWishItem(ahmedId, wishId2);
        System.out.println("Deleted successfully");

        System.out.println("\n=== 12b. DELETE_WISH_ITEM: try deleting wishId (has contributions, should fail) ===");
        try {
            wishlistService.deleteWishItem(ahmedId, wishId);
            System.out.println("ERROR: should have thrown!");
        } catch (IllegalStateException e) {
            System.out.println("Correctly rejected: " + e.getMessage());
        }

        System.out.println("\n=== 6. REMOVE_FRIEND: ahmed removes mohamed ===");
        friendService.removeFriend(ahmedId, mohamedId);
        System.out.println("Removed. Are they still friends? Should be false in next VIEW_FRIENDS call.");
        System.out.println(friendService.viewFriends(ahmedId));

        System.out.println("\n=== DONE. Review the output above — if every '===' section looks correct, ===");
        System.out.println("=== the business logic layer is ready for Person 2 to wire up.           ===");
    }
}