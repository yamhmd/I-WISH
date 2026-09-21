package iwish.db;

import iwish.model.WishItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * All queries touching the WishItems table.
 *
 * RULE (agreed with the team, see PROTOCOL.md notes): a wish item that
 * already has contributions cannot be edited or deleted. updateWishItem()
 * and deleteWishItem() both enforce this by checking Contributions first
 * and throwing IllegalStateException if any exist. Person 3 should catch
 * that and map it to the matching PROTOCOL.md error message.
 */
public class WishItemDAO {

    /** Throws IllegalStateException if this catalog item is already on the user's wish list. */
    public WishItem addWishItem(int userId, int itemId) throws SQLException {
        if (isItemAlreadyOnWishlist(userId, itemId)) {
            throw new IllegalStateException("Item already in wish list");
        }

        String sql = "INSERT INTO WishItems (user_id, item_id, amount_raised, is_complete) VALUES (?, ?, 0.00, FALSE)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setInt(2, itemId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int wishId = keys.getInt(1);
                    return new WishItem(wishId, userId, itemId, java.math.BigDecimal.ZERO, false);
                }
            }
            throw new SQLException("WishItem insert did not return a generated key");
        }
    }

    /**
     * Deletes a wish item — only if it belongs to userId AND has no contributions.
     * @return true if deleted, false if it didn't exist or didn't belong to this user
     * @throws IllegalStateException if it has contributions
     */
    public boolean deleteWishItem(int userId, int wishId) throws SQLException {
        if (hasContributions(wishId)) {
            throw new IllegalStateException("Cannot delete an item with existing contributions");
        }
        String sql = "DELETE FROM WishItems WHERE wish_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, wishId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Changes which catalog item a wish entry points to — only if it
     * belongs to userId AND has no contributions.
     * @return true if updated, false if it didn't exist or didn't belong to this user
     * @throws IllegalStateException if it has contributions
     */
    public boolean updateWishItem(int userId, int wishId, int newItemId) throws SQLException {
        if (hasContributions(wishId)) {
            throw new IllegalStateException("Cannot edit an item with existing contributions");
        }
        String sql = "UPDATE WishItems SET item_id = ? WHERE wish_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newItemId);
            ps.setInt(2, wishId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /** All wish items for a user, joined with catalog item name/price for display. */
    public List<WishItem> getWishItemsByUser(int userId) throws SQLException {
        return queryWishItems("WHERE w.user_id = ?", userId);
    }

    /** Same as getWishItemsByUser but intended for viewing a FRIEND's list.
     *  Caller (business logic) must check FriendDAO.areFriends() first —
     *  this method does not itself verify the friendship, it only fetches data. */
    public List<WishItem> getFriendWishlist(int friendId) throws SQLException {
        return queryWishItems("WHERE w.user_id = ?", friendId);
    }

    /** Returns null if no wish item with that id exists. */
    public WishItem getWishItemById(int wishId) throws SQLException {
        List<WishItem> results = queryWishItems("WHERE w.wish_id = ?", wishId);
        return results.isEmpty() ? null : results.get(0);
    }

    public boolean hasContributions(int wishId) throws SQLException {
        String sql = "SELECT 1 FROM Contributions WHERE wish_id = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, wishId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isItemAlreadyOnWishlist(int userId, int itemId) throws SQLException {
        String sql = "SELECT 1 FROM WishItems WHERE user_id = ? AND item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<WishItem> queryWishItems(String whereClause, int param) throws SQLException {
        String sql =
            "SELECT w.*, c.name AS item_name, c.price AS item_price " +
            "FROM WishItems w JOIN CatalogItems c ON w.item_id = c.item_id " +
            whereClause;

        List<WishItem> items = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WishItem w = new WishItem(
                        rs.getInt("wish_id"),
                        rs.getInt("user_id"),
                        rs.getInt("item_id"),
                        rs.getBigDecimal("amount_raised"),
                        rs.getBoolean("is_complete")
                    );
                    w.setItemName(rs.getString("item_name"));
                    w.setItemPrice(rs.getBigDecimal("item_price"));
                    items.add(w);
                }
            }
        }
        return items;
    }
}
