package iwish.model;

import java.sql.Timestamp;

public class Friend {
    private int friendId;
    private int userId;       // who sent the request
    private int friendUserId; // who received the request
    private String status;    // "pending" or "accepted"
    private Timestamp requestedAt;

    // Populated only on queries that join Users for display purposes
    // (e.g. "who is this pending request from?"). Null otherwise.
    private String otherUsername;

    public Friend(int friendId, int userId, int friendUserId, String status, Timestamp requestedAt) {
        this.friendId = friendId;
        this.userId = userId;
        this.friendUserId = friendUserId;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    public int getFriendId() { return friendId; }
    public int getUserId() { return userId; }
    public int getFriendUserId() { return friendUserId; }
    public String getStatus() { return status; }
    public Timestamp getRequestedAt() { return requestedAt; }

    public String getOtherUsername() { return otherUsername; }
    public void setOtherUsername(String otherUsername) { this.otherUsername = otherUsername; }
}
