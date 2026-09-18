package iwish.model;

import java.sql.Timestamp;

public class Notification {
    private int notifId;
    private int userId;
    private String message;
    private String type; // "buyer" or "receiver"
    private boolean read;
    private Timestamp createdAt;

    public Notification(int notifId, int userId, String message, String type, boolean read, Timestamp createdAt) {
        this.notifId = notifId;
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.read = read;
        this.createdAt = createdAt;

    }

    public int getNotifId() { return notifId; }
    public int getUserId() { return userId; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return read; }
    public Timestamp getCreatedAt() { return createdAt; }
}
