package iwish.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Edge-case / error-path tests for the Service layer.
 * Prints PASS or FAIL for every case and a summary at the end.
 *
 * Usernames get a unique suffix, so this can be re-run WITHOUT resetting the
 * database (schema.sql + seed.sql must have been run at least once).
 */
public class ServiceEdgeCaseTest {

    private static int passed = 0;
    private static int failed = 0;

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private static void pass(String label) {
        passed++;
        System.out.println("  PASS  " + label);
    }

    private static void fail(String label, String why) {
        failed++;
        System.out.println("  FAIL  " + label + "  -> " + why);
    }

    private static void expectError(String label, String expected, Action a) {
        try {
            a.run();
            fail(label, "no error thrown (expected \"" + expected + "\")");
        } catch (IllegalStateException e) {
            if (expected.equals(e.getMessage())) {
                pass(label);
            } else {
                fail(label, "got \"" + e.getMessage() + "\" but expected \"" + expected + "\"");
            }
        } catch (Exception e) {
            fail(label, "unexpected " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void expectOk(String label, Action a) {
        try {
            a.run();
            pass(label);
        } catch (Exception e) {
            fail(label, "threw " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void expectTrue(String label, boolean condition) {
        if (condition) {
            pass(label);
        } else {
            fail(label, "condition was false");
        }
    }

    private static int itemId(List<Map<String, Object>> catalog, String name) {
        Map<String, Object> found = catalog.stream()
                .filter(i -> name.equals(i.get("name")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Run seed.sql first: '" + name + "' not found"));
        return (int) found.get("item_id");
    }

    public static void main(String[] args) throws Exception {
        UserService users = new UserService();
        FriendService friends = new FriendService();
        WishlistService wishlist = new WishlistService();
        ContributionService contributions = new ContributionService();
        NotificationService notifications = new NotificationService();

        final String s = String.valueOf(System.currentTimeMillis() % 1000000);
        final String aliceName = "alice_" + s;
        final String bobName = "bob_" + s;
        final String carolName = "carol_" + s;
        final String daveName = "dave_" + s;

        System.out.println("=== SETUP ===");
        int aliceId = users.register(aliceName, aliceName + "@mail.com", "pass123");
        int bobId = users.register(bobName, bobName + "@mail.com", "pass123");
        int carolId = users.register(carolName, carolName + "@mail.com", "pass123");
        int daveId = users.register(daveName, daveName + "@mail.com", "pass123");

        friends.addFriend(bobId, aliceName);          // bob <-> alice : friends
        friends.acceptFriend(aliceId, bobId);
        friends.addFriend(carolId, aliceName);        // carol -> alice : still PENDING
        // dave : no relationship with anyone

        List<Map<String, Object>> catalog = wishlist.viewCatalog();
        int headphonesId = itemId(catalog, "Headphones");
        int sneakersId = itemId(catalog, "Sneakers");
        int sunglassesId = itemId(catalog, "Sunglasses");
        int perfumeId = itemId(catalog, "Perfume");
        System.out.println("alice=" + aliceId + " bob=" + bobId + " carol=" + carolId + " dave=" + daveId);

        // ------------------------------------------------------------
        System.out.println("\n=== USERS ===");
        expectError("register: duplicate username", "Username already taken",
                () -> users.register(aliceName, "other_" + s + "@mail.com", "pass123"));
        expectError("register: duplicate email", "Email already registered",
                () -> users.register("other_" + s, aliceName + "@mail.com", "pass123"));
        expectError("register: empty username", "Username is required",
                () -> users.register("   ", "x" + s + "@mail.com", "pass123"));
        expectError("register: invalid email", "Invalid email",
                () -> users.register("bad_" + s, "not-an-email", "pass123"));
        expectError("register: short password", "Password must be at least 6 characters",
                () -> users.register("short_" + s, "short" + s + "@mail.com", "123"));
        expectError("login: unknown user", "Invalid credentials",
                () -> users.login("nobody_" + s, "pass123"));

        // ------------------------------------------------------------
        System.out.println("\n=== FRIENDS ===");
        expectError("add friend: user does not exist", "User not found",
                () -> friends.addFriend(aliceId, "no_such_user_" + s));
        expectError("add friend: yourself", "Cannot add yourself as a friend",
                () -> friends.addFriend(aliceId, aliceName));
        expectError("add friend: already friends", "Already friends",
                () -> friends.addFriend(bobId, aliceName));
        expectError("add friend: duplicate pending request", "Friend request already pending",
                () -> friends.addFriend(carolId, aliceName));
        expectError("add friend: reverse of a pending request", "Friend request already pending",
                () -> friends.addFriend(aliceId, carolName));
        expectError("accept: no such request", "Friend request not found",
                () -> friends.acceptFriend(aliceId, daveId));
        expectError("decline: no such request", "Friend request not found",
                () -> friends.declineFriend(aliceId, daveId));
        expectError("remove: not friends", "Not friends",
                () -> friends.removeFriend(aliceId, daveId));

        // ------------------------------------------------------------
        System.out.println("\n=== WISH LIST ===");
        expectOk("create wish: alice adds Headphones",
                () -> wishlist.createWishItem(aliceId, headphonesId));
        expectError("create wish: same item twice", "Item already in wish list",
                () -> wishlist.createWishItem(aliceId, headphonesId));
        expectError("create wish: item_id does not exist", "Item not found",
                () -> wishlist.createWishItem(aliceId, 999999));

        Map<String, Object> mine = wishlist.viewMyWishlist(aliceId);
        List<?> myItems = (List<?>) mine.get("wish_items");
        expectTrue("view MY wish list: shows 1 item", myItems.size() == 1);
        int headphonesWishId = (int) ((Map<?, ?>) myItems.get(0)).get("wish_id");

        expectOk("update wish: no contributions -> allowed",
                () -> wishlist.updateWishItem(aliceId, headphonesWishId, sneakersId));
        expectError("update wish: new item does not exist", "Item not found",
                () -> wishlist.updateWishItem(aliceId, headphonesWishId, 999999));
        expectError("update wish: someone else's wish", "Item not found",
                () -> wishlist.updateWishItem(bobId, headphonesWishId, perfumeId));
        expectError("delete wish: someone else's wish", "Item not found",
                () -> wishlist.deleteWishItem(bobId, headphonesWishId));
        expectError("view friend's list: stranger", "Not friends",
                () -> wishlist.viewFriendWishlist(daveId, aliceId));
        expectError("view friend's list: pending request is not friendship", "Not friends",
                () -> wishlist.viewFriendWishlist(carolId, aliceId));

        // ------------------------------------------------------------
        System.out.println("\n=== CONTRIBUTIONS (Sunglasses, price 250) ===");
        int sunglassesWishId = (int) wishlist.createWishItem(aliceId, sunglassesId).get("wish_id");

        expectError("contribute: amount 0", "Invalid amount",
                () -> contributions.contribute(bobId, sunglassesWishId, new BigDecimal("0")));
        expectError("contribute: negative amount", "Invalid amount",
                () -> contributions.contribute(bobId, sunglassesWishId, new BigDecimal("-50")));
        expectError("contribute: 3 decimal places", "Invalid amount",
                () -> contributions.contribute(bobId, sunglassesWishId, new BigDecimal("10.555")));
        expectError("contribute: null amount", "Invalid amount",
                () -> contributions.contribute(bobId, sunglassesWishId, null));
        expectError("contribute: wish does not exist", "Item not found",
                () -> contributions.contribute(bobId, 999999, new BigDecimal("10")));
        expectError("contribute: stranger (not a friend)", "Not friends",
                () -> contributions.contribute(daveId, sunglassesWishId, new BigDecimal("10")));
        expectError("contribute: only a pending request (not friends yet)", "Not friends",
                () -> contributions.contribute(carolId, sunglassesWishId, new BigDecimal("10")));
        expectError("contribute: to your own wish", "Cannot contribute to your own wish item",
                () -> contributions.contribute(aliceId, sunglassesWishId, new BigDecimal("10")));
        expectError("contribute: more than the price", "Amount exceeds remaining price",
                () -> contributions.contribute(bobId, sunglassesWishId, new BigDecimal("300")));

        Map<String, Object> r1 = contributions.contribute(bobId, sunglassesWishId, new BigDecimal("100.00"));
        expectTrue("contribute: 100 -> not complete yet", Boolean.FALSE.equals(r1.get("wish_completed")));
        Map<String, Object> r2 = contributions.contribute(bobId, sunglassesWishId, new BigDecimal("100.00"));
        expectTrue("contribute: same friend again, 200/250 -> not complete", Boolean.FALSE.equals(r2.get("wish_completed")));

        expectError("update wish: has contributions", "Cannot edit an item with existing contributions",
                () -> wishlist.updateWishItem(aliceId, sunglassesWishId, perfumeId));
        expectError("delete wish: has contributions", "Cannot delete an item with existing contributions",
                () -> wishlist.deleteWishItem(aliceId, sunglassesWishId));
        expectError("contribute: 100 when only 50 remains", "Amount exceeds remaining price",
                () -> contributions.contribute(bobId, sunglassesWishId, new BigDecimal("100")));

        Map<String, Object> r3 = contributions.contribute(bobId, sunglassesWishId, new BigDecimal("50.00"));
        expectTrue("contribute: last 50 -> wish completed", Boolean.TRUE.equals(r3.get("wish_completed")));
        expectError("contribute: after completion", "Item already complete",
                () -> contributions.contribute(bobId, sunglassesWishId, new BigDecimal("10")));

        // ------------------------------------------------------------
        System.out.println("\n=== NOTIFICATIONS ===");
        List<Map<String, Object>> aliceNotifs = notifications.getNotifications(aliceId);
        Map<String, Object> receiverNotif = aliceNotifs.stream()
                .filter(n -> "receiver".equals(n.get("type")))
                .findFirst()
                .orElse(null);
        expectTrue("owner gets a 'receiver' notification", receiverNotif != null);
        expectTrue("owner's message names the friend who bought it (spec #9)",
                receiverNotif != null && String.valueOf(receiverNotif.get("message")).contains(bobName));

        long bobBuyerCount = notifications.getNotifications(bobId).stream()
                .filter(n -> "buyer".equals(n.get("type")))
                .count();
        expectTrue("contributor who paid 3 times gets exactly 1 'buyer' notification", bobBuyerCount == 1);

        if (receiverNotif != null) {
            int aliceNotifId = (int) receiverNotif.get("notif_id");
            expectError("mark read: someone else's notification", "Notification not found",
                    () -> notifications.markNotificationRead(bobId, aliceNotifId));
        }
        expectError("mark read: notification does not exist", "Notification not found",
                () -> notifications.markNotificationRead(aliceId, 999999));

        // ------------------------------------------------------------
        System.out.println("\n=== SUMMARY ===");
        System.out.println("Passed: " + passed + "   Failed: " + failed);
        if (failed > 0) {
            System.out.println("Send me the FAIL lines above and I'll tell you which layer needs the fix.");
        }
    }
}