package iwish.db;

import iwish.model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    /** type must be "buyer" or "receiver" — matches the ENUM in the schema. */
    public void addNotification(int userId, String message, String type) throws SQLException {
        String sql = "INSERT INTO Notifications (user_id, message, type, is_read) VALUES (?, ?, ?, FALSE)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.setString(3, type);
            ps.executeUpdate();
        }
    }

    /** All notifications for a user, newest first. */
    public List<Notification> getNotifications(int userId) throws SQLException {
        String sql = "SELECT * FROM Notifications WHERE user_id = ? ORDER BY created_at DESC";
        List<Notification> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new Notification(
                        rs.getInt("notif_id"),
                        rs.getInt("user_id"),
                        rs.getString("message"),
                        rs.getString("type"),
                        rs.getBoolean("is_read"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return results;
    }

    /**
     * Marks a notification read — only if it belongs to userId, so one
     * user can't mark another user's notification as read.
     * @return true if a matching notification was found and updated
     */
    public boolean markAsRead(int userId, int notifId) throws SQLException {
        String sql = "UPDATE Notifications SET is_read = TRUE WHERE notif_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notifId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
