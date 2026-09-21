package com.iwish.iwishclient.network;

import com.iwish.iwishclient.model.FriendRequest;
import com.iwish.iwishclient.model.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MockApiClient implements ApiClient {

    private static class Account {
        final int id;
        final String username;
        final String email;
        final String password;

        Account(int id, String username, String email, String password) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.password = password;
        }
    }

    private final Map<String, Account> byUsername = new HashMap<>();
    private final Map<Integer, Account> byId = new HashMap<>();
    private final Set<String> friendships = new HashSet<>();
    private final List<int[]> pending = new ArrayList<>(); // {requesterId, targetId}
    private int nextId = 1;

    public MockApiClient() {
        addAccount("ahmed", "ahmed@mail.com", "password123");     // id 1
        addAccount("mohamed", "mohamed@mail.com", "password123"); // id 2
        addAccount("sara", "sara@mail.com", "password123");       // id 3
        friendships.add(key(1, 2)); // ahmed and mohamed are friends
        pending.add(new int[]{3, 1}); // sara sent a request to ahmed
    }

    private Account addAccount(String username, String email, String password) {
        Account a = new Account(nextId++, username, email, password);
        byUsername.put(username, a);
        byId.put(a.id, a);
        return a;
    }

    private static String key(int a, int b) {
        return Math.min(a, b) + "-" + Math.max(a, b);
    }

    private void fakeDelay() {
        try {
            Thread.sleep(600);
        } catch (InterruptedException ignored) {
        }
    }

    private boolean hasPendingBetween(int a, int b) {
        for (int[] p : pending) {
            if ((p[0] == a && p[1] == b) || (p[0] == b && p[1] == a)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized User register(String username, String email, String password) throws ApiException {
        fakeDelay();
        if (byUsername.containsKey(username)) {
            throw new ApiException("Username already taken");
        }
        Account a = addAccount(username, email, password);
        return new User(a.id, a.username);
    }

    @Override
    public synchronized User login(String username, String password) throws ApiException {
        fakeDelay();
        Account a = byUsername.get(username);
        if (a == null || !a.password.equals(password)) {
            throw new ApiException("Invalid credentials");
        }
        return new User(a.id, a.username);
    }

    @Override
    public synchronized void addFriend(int userId, String friendUsername) throws ApiException {
        fakeDelay();
        Account target = byUsername.get(friendUsername);
        if (target == null) {
            throw new ApiException("User not found");
        }
        if (target.id == userId) {
            throw new ApiException("Cannot add yourself as a friend");
        }
        if (friendships.contains(key(userId, target.id))) {
            throw new ApiException("Already friends");
        }
        if (hasPendingBetween(userId, target.id)) {
            throw new ApiException("Friend request already pending");
        }
        pending.add(new int[]{userId, target.id});
    }

    @Override
    public synchronized void acceptFriend(int userId, int requesterId) throws ApiException {
        fakeDelay();
        boolean removed = pending.removeIf(p -> p[0] == requesterId && p[1] == userId);
        if (!removed) {
            throw new ApiException("Friend request not found");
        }
        friendships.add(key(userId, requesterId));
    }

    @Override
    public synchronized void declineFriend(int userId, int requesterId) throws ApiException {
        fakeDelay();
        boolean removed = pending.removeIf(p -> p[0] == requesterId && p[1] == userId);
        if (!removed) {
            throw new ApiException("Friend request not found");
        }
    }

    @Override
    public synchronized void removeFriend(int userId, int friendId) throws ApiException {
        fakeDelay();
        if (!friendships.remove(key(userId, friendId))) {
            throw new ApiException("Not friends");
        }
    }

    @Override
    public synchronized List<User> viewFriends(int userId) throws ApiException {
        fakeDelay();
        List<User> result = new ArrayList<>();
        for (Account a : byId.values()) {
            if (a.id != userId && friendships.contains(key(userId, a.id))) {
                result.add(new User(a.id, a.username));
            }
        }
        return result;
    }

    @Override
    public synchronized List<FriendRequest> viewPendingRequests(int userId) throws ApiException {
        fakeDelay();
        List<FriendRequest> result = new ArrayList<>();
        for (int[] p : pending) {
            if (p[1] == userId) {
                result.add(new FriendRequest(p[0], byId.get(p[0]).username, Instant.now().toString()));
            }
        }
        return result;
    }
}
