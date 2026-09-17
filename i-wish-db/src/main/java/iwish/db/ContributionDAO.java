package iwish.db;

import java.math.BigDecimal;
import java.sql.*;

/**
 * Handles contributions to a wish item.
 *
 * *** THIS IS THE MOST IMPORTANT CLASS IN THE PROJECT. ***
 *
 * The scenario we must prevent: two people contribute to the same item
 * at nearly the same moment, and both contributions get accepted even
 * though together they push the total PAST the item's price. Without
 * protection, two threads could both read "amount_raised = 400, price =
 * 500", both decide "my 200 fits", and both write — ending at 800/500.
 *
 * The fix used here: a single guarded UPDATE statement that only
 * succeeds if the new total still fits within the price, checked and
 * written atomically by MySQL itself (not by our Java code reading-then-
 * writing in two separate steps). We also wrap the update + the
 * Contributions insert in one JDBC transaction so they either both
 * happen or neither does.
 *
 * DO NOT "simplify" this into a separate SELECT to check the amount
 * followed by an UPDATE — that reintroduces exactly the race condition
 * this class exists to prevent.
 */
public class ContributionDAO {

    /** Result of attempting a contribution. */
    public static class ContributionResult {
        public final boolean success;
        public final boolean wishCompleted;
        public final String errorMessage; // null if success

        private ContributionResult(boolean success, boolean wishCompleted, String errorMessage) {
            this.success = success;
            this.wishCompleted = wishCompleted;
            this.errorMessage = errorMessage;
        }

        public static ContributionResult ok(boolean completed) {
            return new ContributionResult(true, completed, null);
        }

        public static ContributionResult error(String message) {
            return new ContributionResult(false, false, message);
        }
    }

    public ContributionResult contribute(int wishId, int contributorId, BigDecimal amount) throws SQLException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ContributionResult.error("Amount must be greater than zero");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // start manual transaction

            try {
                // Step 1: the guarded, atomic update. This single SQL
                // statement checks "does this fit within the price?" AND
                // writes the new total in the same operation, so no other
                // thread can slip in between the check and the write.
                String guardedUpdateSql =
                    "UPDATE WishItems " +
                    "SET amount_raised = amount_raised + ? " +
                    "WHERE wish_id = ? AND amount_raised + ? <= " +
                    "  (SELECT price FROM CatalogItems c WHERE c.item_id = WishItems.item_id) " +
                    "AND is_complete = FALSE";

                int rowsAffected;
                try (PreparedStatement ps = conn.prepareStatement(guardedUpdateSql)) {
                    ps.setBigDecimal(1, amount);
                    ps.setInt(2, wishId);
                    ps.setBigDecimal(3, amount);
                    rowsAffected = ps.executeUpdate();
                }

                if (rowsAffected == 0) {
                    // Either: wish_id doesn't exist, item is already complete,
                    // or this amount would exceed the remaining price.
                    conn.rollback();
                    return ContributionResult.error("Amount exceeds remaining price, or item not found / already complete");
                }

                // Step 2: record the contribution row itself.
                String insertSql =
                    "INSERT INTO Contributions (wish_id, contributor_id, amount) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setInt(1, wishId);
                    ps.setInt(2, contributorId);
                    ps.setBigDecimal(3, amount);
                    ps.executeUpdate();
                }

                // Step 3: check whether this contribution just completed the item.
                // (amount_raised == price now). If so, flip is_complete to TRUE.
                boolean justCompleted = checkAndMarkComplete(conn, wishId);

                conn.commit();
                return ContributionResult.ok(justCompleted);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Must be called within the same transaction/connection as the update
     * above. Checks if amount_raised has reached price; if so, sets
     * is_complete = TRUE and returns true (meaning: this call is what
     * completed it — trigger notifications). Returns false if it was
     * already complete before this call, or isn't complete yet.
     */
    private boolean checkAndMarkComplete(Connection conn, int wishId) throws SQLException {
        String sql =
            "UPDATE WishItems w " +
            "JOIN CatalogItems c ON w.item_id = c.item_id " +
            "SET w.is_complete = TRUE " +
            "WHERE w.wish_id = ? AND w.is_complete = FALSE AND w.amount_raised >= c.price";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, wishId);
            int rows = ps.executeUpdate();
            return rows > 0; // true only if THIS call flipped it from false to true
        }
    }

    /** Returns the list of distinct contributor user_ids for a wish item — used to notify all buyers when it completes. */
    public java.util.List<Integer> getContributorIds(int wishId) throws SQLException {
        String sql = "SELECT DISTINCT contributor_id FROM Contributions WHERE wish_id = ?";
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, wishId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("contributor_id"));
                }
            }
        }
        return ids;
    }
}
