package com.iwish.iwishclient.model;

public class FriendRequest {
    private final int requesterId;
    private final String username;
    private final String requestedAt;

    public FriendRequest(int requesterId, String username, String requestedAt) {
        this.requesterId = requesterId;
        this.username = username;
        this.requestedAt = requestedAt;
    }

    public int getRequesterId() { return requesterId; }
    public String getUsername() { return username; }
    public String getRequestedAt() { return requestedAt; }
}
