package com.iwish.iwishclient.model;

/** One entry from PROTOCOL.md #16 GET_NOTIFICATIONS. "type" is "buyer" or "receiver". */
public class NotificationItem {
    private final int notifId;
    private final String type;
    private final String message;
    private final boolean read;
    private final String createdAt;

    public NotificationItem(int notifId, String type, String message, boolean read, String createdAt) {
        this.notifId = notifId;
        this.type = type;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getNotifId() { return notifId; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public String getCreatedAt() { return createdAt; }
}
