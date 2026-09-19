package iwish.db;

import iwish.model.*;
import iwish.util.PasswordUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * Run this class directly (right-click -> Run) to sanity-check the
 * entire database layer BEFORE Person 3 starts building business logic
 * on top of it, and BEFORE Person 2 wires up the iwish.server.
 *
 * What it does, step by step, printing results as it goes:
 *   1. Creates 3 iwish.test users (with properly hashed passwords)
 *   2. Sends and accepts a friend request between two of them
 *   3. Adds a wish item to one user's list
 *   4. Has the other two users contribute to it (deliberately completing it)
 *   5. Confirms notifications were created for the right people
 *
 * If this runs cleanly with no exceptions and the printed output makes
 * sense, the database layer is solid and ready to be handed off.
 *
 * PREREQUISITE: you must have already run schema.sql and seed.sql
 * against your local MySQL instance (see the setup instructions).
 */
public class DBTestMain {

    public static void main(String[] args) throws Exception {
        UserDAO userDAO = new UserDAO();
        FriendDAO friendDAO = new FriendDAO();
        CatalogItemDAO catalogDAO = new CatalogItemDAO();
        WishItemDAO wishItemDAO = new WishItemDAO();
        ContributionDAO contributionDAO = new ContributionDAO();
        NotificationDAO notificationDAO = new NotificationDAO();

        System.out.println("=== 1. Creating iwish.test users ===");
        int ahmedId = createTestUser(userDAO, "ahmed", "ahmed@mail.com");
        int mohamedId = createTestUser(userDAO, "mohamed", "mohamed@mail.com");
        int saraId = createTestUser(userDAO, "sara", "sara@mail.com");
        System.out.println("ahmed=" + ahmedId + " mohamed=" + mohamedId + " sara=" + saraId);

        System.out.println("\n=== 2. Login check (correct + wrong password) ===");
        User loginAttempt = userDAO.findByUsername("ahmed");
        System.out.println("Correct password verifies: " + PasswordUtil.verify("password123", loginAttempt.getPasswordHash()));
        System.out.println("Wrong password verifies:   " + PasswordUtil.verify("wrongpass", loginAttempt.getPasswordHash()));

        System.out.println("\n=== 3. Friend request: mohamed -> ahmed, then ahmed accepts ===");
        friendDAO.sendRequest(mohamedId, "ahmed");
        List<Friend> pending = friendDAO.getPendingRequests(ahmedId);
        System.out.println("Ahmed's pending requests: " + pending.size() + " (from " +
            (pending.isEmpty() ? "none" : pending.get(0).getOtherUsername()) + ")");
        boolean accepted = friendDAO.acceptRequest(ahmedId, mohamedId);
        System.out.println("Accepted: " + accepted);
        System.out.println("Are they friends now? " + friendDAO.areFriends(ahmedId, mohamedId));

        System.out.println("\n=== 4. Ahmed adds an item to his wish list ===");
        List<CatalogItem> catalog = catalogDAO.getAllItems();
        CatalogItem headphones = catalog.stream()
            .filter(i -> i.getName().equals("Headphones"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Run seed.sql first — 'Headphones' not found in catalog"));
        WishItem wish = wishItemDAO.addWishItem(ahmedId, headphones.getItemId());
        System.out.println("Created wish_id=" + wish.getWishId() + " for item '" + headphones.getName() +
            "' price=" + headphones.getPrice());

        System.out.println("\n=== 5. Mohamed contributes 200, Sara contributes 300 (should complete it) ===");
        // Note: mohamed and sara are NOT the list owner (ahmed) — contributing
        // to your own item should be blocked at the business-logic layer
        // (Person 3), not here — this DAO layer doesn't know who "owns"
        // the social meaning of a contribution, only the math.
        ContributionDAO.ContributionResult r1 = contributionDAO.contribute(wish.getWishId(), mohamedId, new BigDecimal("200.00"));
        System.out.println("Mohamed's 200 contribution -> success=" + r1.success + " completed=" + r1.wishCompleted +
            (r1.errorMessage != null ? " error=" + r1.errorMessage : ""));

        ContributionDAO.ContributionResult r2 = contributionDAO.contribute(wish.getWishId(), saraId, new BigDecimal("300.00"));
        System.out.println("Sara's 300 contribution -> success=" + r2.success + " completed=" + r2.wishCompleted +
            (r2.errorMessage != null ? " error=" + r2.errorMessage : ""));

        System.out.println("\n=== 6. Try to over-contribute after completion (should fail) ===");
        ContributionDAO.ContributionResult r3 = contributionDAO.contribute(wish.getWishId(), saraId, new BigDecimal("50.00"));
        System.out.println("Extra 50 contribution -> success=" + r3.success +
            (r3.errorMessage != null ? " error=" + r3.errorMessage : ""));

        System.out.println("\n=== 7. Simulating notification triggers (this logic normally lives in Person 3's code) ===");
        if (r2.wishCompleted) {
            for (Integer contributorId : contributionDAO.getContributorIds(wish.getWishId())) {
                notificationDAO.addNotification(contributorId,
                    "Your contribution to '" + headphones.getName() + "' helped complete it!", "buyer");
            }
            notificationDAO.addNotification(ahmedId,
                "Your item '" + headphones.getName() + "' was fully funded!", "receiver");
        }

        System.out.println("\n=== 8. Reading back notifications ===");
        printNotifications("Ahmed", notificationDAO.getNotifications(ahmedId));
        printNotifications("Mohamed", notificationDAO.getNotifications(mohamedId));
        printNotifications("Sara", notificationDAO.getNotifications(saraId));

        System.out.println("\n=== DONE. If everything above looks correct, the DB layer is ready. ===");
    }

    private static int createTestUser(UserDAO userDAO, String username, String email) throws Exception {
        String hash = PasswordUtil.hash("password123");
        return userDAO.createUser(username, email, hash);
    }

    private static void printNotifications(String label, List<Notification> notifications) {
        System.out.println(label + " has " + notifications.size() + " notification(s):");
        for (Notification n : notifications) {
            System.out.println("  [" + n.getType() + "] " + n.getMessage());
        }
    }
}
