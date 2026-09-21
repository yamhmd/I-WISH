package com.iwish.iwishclient.network;

import com.iwish.iwishclient.model.CatalogItem;
import com.iwish.iwishclient.model.FriendRequest;
import com.iwish.iwishclient.model.FriendWishlist;
import com.iwish.iwishclient.model.NotificationItem;
import com.iwish.iwishclient.model.User;
import com.iwish.iwishclient.model.WishItem;

import java.util.List;

public interface ApiClient {
    User register(String username, String email, String password) throws ApiException;
    User login(String username, String password) throws ApiException;

    void addFriend(int userId, String friendUsername) throws ApiException;
    void acceptFriend(int userId, int requesterId) throws ApiException;
    void declineFriend(int userId, int requesterId) throws ApiException;
    void removeFriend(int userId, int friendId) throws ApiException;
    List<User> viewFriends(int userId) throws ApiException;
    List<FriendRequest> viewPendingRequests(int userId) throws ApiException;

    List<CatalogItem> viewCatalog() throws ApiException;
    WishItem createWishItem(int userId, int itemId) throws ApiException;
    void updateWishItem(int userId, int wishId, int itemId) throws ApiException;
    void deleteWishItem(int userId, int wishId) throws ApiException;
    List<WishItem> viewMyWishlist(int userId) throws ApiException;
    FriendWishlist viewFriendWishlist(int userId, int friendId) throws ApiException;

    boolean contribute(int userId, int wishId, double amount) throws ApiException;
    List<NotificationItem> getNotifications(int userId) throws ApiException;
    void markNotificationRead(int userId, int notifId) throws ApiException;
}
