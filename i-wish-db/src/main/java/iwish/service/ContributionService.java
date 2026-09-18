package iwish.service;

import iwish.db.ContributionDAO;
import iwish.db.FriendDAO;
import iwish.db.NotificationDAO;
import iwish.db.UserDAO;
import iwish.db.WishItemDAO;
import iwish.model.User;
import iwish.model.WishItem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class ContributionService {

    private final ContributionDAO contributionDAO = new ContributionDAO();
    private final WishItemDAO wishItemDAO = new WishItemDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final FriendDAO friendDAO = new FriendDAO();
    private final UserDAO userDAO = new UserDAO();

    // CONTRIBUTE
    public Map<String, Object> contribute(int userId, int wishId, BigDecimal amount) throws SQLException {

        // Must be positive and at most 2 decimal places. Without this a negative
        // amount would pass the DAO's guard (amount_raised + ? <= price) and
        // REDUCE the money raised.
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalStateException("Invalid amount");
        }

        // Look up first so we can tell apart the distinct protocol errors —
        // the DAO's guarded update can't distinguish them on its own.
        WishItem item = wishItemDAO.getWishItemById(wishId);

        if (item == null) {
            throw new IllegalStateException("Item not found");
        }
        if (item.getUserId() == userId) {
            throw new IllegalStateException("Cannot contribute to your own wish item");
        }
        // Only friends of the list owner may contribute.
        if (!friendDAO.areFriends(userId, item.getUserId())) {
            throw new IllegalStateException("Not friends");
        }
        if (item.isComplete()) {
            throw new IllegalStateException("Item already complete");
        }

        ContributionDAO.ContributionResult result = contributionDAO.contribute(wishId, userId, amount);

        if (!result.success) {
            // "not found" and "already complete" are ruled out above,
            // so this must be the price guard.
            throw new IllegalStateException("Amount exceeds remaining price");
        }

        if (result.wishCompleted) {
            // The contribution is already saved at this point. If sending the
            // notifications fails we must NOT report the whole call as an error,
            // or the client would think the money was not recorded.
            try {
                notifyOnCompletion(item, wishId);
            } catch (SQLException e) {
                System.err.println("WARNING: contribution saved but notifications failed: " + e.getMessage());
            }
        }

        return Map.of("wish_completed", result.wishCompleted);
    }

    private void notifyOnCompletion(WishItem item, int wishId) throws SQLException {
        // De-duplicate: someone who contributed 3 times gets ONE notification.
        List<Integer> contributorIds =
                new ArrayList<>(new LinkedHashSet<>(contributionDAO.getContributorIds(wishId)));

        List<String> names = new ArrayList<>();
        for (int contributorId : contributorIds) {
            notificationDAO.addNotification(
                    contributorId,
                    "Your contribution to '" + item.getItemName() + "' helped complete it!",
                    "buyer"
            );
            User u = userDAO.findById(contributorId);
            if (u != null) {
                names.add(u.getUsername());
            }
        }

        // Spec #9: the receiver is told which friend(s) bought the item.
        String message = "Your item '" + item.getItemName() + "' was fully funded by "
                + String.join(", ", names) + "!";
        if (message.length() > 255) {              // Notifications.message is VARCHAR(255)
            message = message.substring(0, 252) + "...";
        }
        notificationDAO.addNotification(item.getUserId(), message, "receiver");
    }
}