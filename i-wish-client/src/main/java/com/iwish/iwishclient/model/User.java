package com.iwish.iwishclient.model;

public class User {
    private final int userId;
    private final String username;

    public User(int userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }

    @Override
    public String toString() { return username; }

    // Servers hand back a fresh User object on every call, so screens that
    // remember "the previously selected friend" (e.g. to restore a ComboBox
    // selection after a refresh) need equality by id, not by reference.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return userId == other.userId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(userId);
    }
}
