package com.iwish.iwishclient.network;

import com.iwish.iwishclient.model.CatalogItem;
import com.iwish.iwishclient.model.FriendRequest;
import com.iwish.iwishclient.model.FriendWishlist;
import com.iwish.iwishclient.model.NotificationItem;
import com.iwish.iwishclient.model.User;
import com.iwish.iwishclient.model.WishItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private final List<int[]> pending = new ArrayList<>();
    private final List<CatalogItem> catalog = new ArrayList<>();
    private final Map<Integer, List<WishItem>> wishlists = new HashMap<>();
    private int nextId = 1;
    private int nextWishId = 1;

    public MockApiClient() {
        addAccount("ahmed", "ahmed@mail.com", "password123");
        addAccount("mohamed", "mohamed@mail.com", "password123");
        addAccount("sara", "sara@mail.com", "password123");
        friendships.add(key(1, 2));
        pending.add(new int[]{3, 1});

        catalog.add(new CatalogItem(10, "Headphones", 500.00));
        catalog.add(new CatalogItem(11, "Watch", 1200.00));
        catalog.add(new CatalogItem(12, "Sneakers", 800.00));

        List<WishItem> mohamedList = new ArrayList<>();
        mohamedList.add(new WishItem(60, 10, "Headphones", 500.00, 200.00, false));
        wishlists.put(2, mohamedList);
    }

    private Account addAccount(String username, String email, String password) {
        Account a = new Account(nextId++, username, email, password);
        byUsername.put(username, a);
        byId.put(a.id, a);
        wishlists.putIfAbsent(a.id, new ArrayList<>());
        return a;
    }

    private static String key(int a, int b) {
        return Math.min(a, b) + "-" + Math.max(a, b);
    }

    private void fakeDelay() {
        try {
            Thread.sleep(400);
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

    private CatalogItem findCatalog(int itemId) {
        for (CatalogItem c : catalog) {
            if (c.getItemId() == itemId) return c;
        }
        return null;
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
        if (target == null) throw new ApiException("User not found");
        if (target.id == userId) throw new ApiException("Cannot add yourself as a friend");
        if (friendships.contains(key(userId, target.id))) throw new ApiException("Already friends");
        if (hasPendingBetween(userId, target.id)) throw new ApiException("Friend request already pending");
        pending.add(new int[]{userId, target.id});
    }

    @Override
    public synchronized void acceptFriend(int userId, int requesterId) throws ApiException {
        fakeDelay();
        boolean removed = pending.removeIf(p -> p[0] == requesterId && p[1] == userId);
        if (!removed) throw new ApiException("Friend request not found");
        friendships.add(key(userId, requesterId));
    }

    @Override
    public synchronized void declineFriend(int userId, int requesterId) throws ApiException {
        fakeDelay();
        boolean removed = pending.removeIf(p -> p[0] == requesterId && p[1] == userId);
        if (!removed) throw new ApiException("Friend request not found");
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

    @Override
    public synchronized List<CatalogItem> viewCatalog() throws ApiException {
        fakeDelay();
        return new ArrayList<>(catalog);
    }

    @Override
    public synchronized WishItem createWishItem(int userId, int itemId) throws ApiException {
        fakeDelay();
        CatalogItem catalogItem = findCatalog(itemId);
        if (catalogItem == null) throw new ApiException("Item not found");
        List<WishItem> list = wishlists.computeIfAbsent(userId, id -> new ArrayList<>());
        for (WishItem w : list) {
            if (w.getItemId() == itemId) throw new ApiException("Item already in wish list");
        }
        WishItem created = new WishItem(nextWishId++, itemId, catalogItem.getName(),
                catalogItem.getPrice(), 0, false);
        list.add(created);
        return created;
    }

    @Override
    public synchronized void updateWishItem(int userId, int wishId, int itemId) throws ApiException {
        fakeDelay();
        CatalogItem catalogItem = findCatalog(itemId);
        if (catalogItem == null) throw new ApiException("Item not found");
        List<WishItem> list = wishlists.getOrDefault(userId, List.of());
        for (int i = 0; i < list.size(); i++) {
            WishItem w = list.get(i);
            if (w.getWishId() == wishId) {
                if (w.hasContributions()) {
                    throw new ApiException("Cannot edit an item with existing contributions");
                }
                list.set(i, new WishItem(wishId, itemId, catalogItem.getName(),
                        catalogItem.getPrice(), 0, false));
                return;
            }
        }
        throw new ApiException("Item not found");
    }

    @Override
    public synchronized void deleteWishItem(int userId, int wishId) throws ApiException {
        fakeDelay();
        List<WishItem> list = wishlists.get(userId);
        if (list == null) throw new ApiException("Item not found");
        Iterator<WishItem> it = list.iterator();
        while (it.hasNext()) {
            WishItem w = it.next();
            if (w.getWishId() == wishId) {
                if (w.hasContributions()) {
                    throw new ApiException("Cannot delete an item with existing contributions");
                }
                it.remove();
                return;
            }
        }
        throw new ApiException("Item not found");
    }

    @Override
    public synchronized List<WishItem> viewMyWishlist(int userId) throws ApiException {
        fakeDelay();
        return new ArrayList<>(wishlists.getOrDefault(userId, List.of()));
    }

    @Override
    public synchronized FriendWishlist viewFriendWishlist(int userId, int friendId) throws ApiException {
        fakeDelay();
        if (!friendships.contains(key(userId, friendId))) {
            throw new ApiException("Not friends");
        }
        Account friend = byId.get(friendId);
        if (friend == null) throw new ApiException("User not found");
        return new FriendWishlist(friend.username,
                new ArrayList<>(wishlists.getOrDefault(friendId, List.of())));
    }

    @Override
    public synchronized boolean contribute(int userId, int wishId, double amount) throws ApiException {
        fakeDelay();
        if (amount <= 0) throw new ApiException("Invalid amount");
        for (Map.Entry<Integer, List<WishItem>> entry : wishlists.entrySet()) {
            List<WishItem> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                WishItem w = list.get(i);
                if (w.getWishId() != wishId) continue;
                if (entry.getKey() == userId) {
                    throw new ApiException("Cannot contribute to your own wish item");
                }
                if (!friendships.contains(key(userId, entry.getKey()))) {
                    throw new ApiException("Not friends");
                }
                if (w.isComplete()) throw new ApiException("Item already complete");
                double remaining = w.getPrice() - w.getAmountRaised();
                if (amount > remaining + 1e-9) {
                    throw new ApiException("Amount exceeds remaining price");
                }
                double raised = w.getAmountRaised() + amount;
                boolean complete = raised + 1e-9 >= w.getPrice();
                list.set(i, new WishItem(w.getWishId(), w.getItemId(), w.getName(),
                        w.getPrice(), raised, complete));
                return complete;
            }
        }
        throw new ApiException("Item not found");
    }

    @Override
    public synchronized List<NotificationItem> getNotifications(int userId) throws ApiException {
        fakeDelay();
        return List.of();
    }

    @Override
    public synchronized void markNotificationRead(int userId, int notifId) throws ApiException {
        fakeDelay();
    }
}
