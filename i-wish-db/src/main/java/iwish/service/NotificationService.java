package iwish.service;

import iwish.db.NotificationDAO;
import iwish.model.Notification;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NotificationService {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    // GET_NOTIFICATIONS
    public List<Map<String, Object>> getNotifications(int userId) throws SQLException {
        List<Notification> notifications = notificationDAO.getNotifications(userId);

        return notifications.stream()
                .map(n -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("notif_id", n.getNotifId());
                    m.put("type", n.getType());
                    m.put("message", n.getMessage());
                    m.put("is_read", n.isRead());
                    m.put("created_at", n.getCreatedAt().toInstant().toString());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // MARK_NOTIFICATION_READ
    public void markNotificationRead(int userId, int notifId) throws SQLException {
        boolean updated = notificationDAO.markAsRead(userId, notifId);
        if (!updated) {
            throw new IllegalStateException("Notification not found");
        }
    }
}