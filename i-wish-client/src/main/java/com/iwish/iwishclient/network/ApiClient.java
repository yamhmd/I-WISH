package com.iwish.iwishclient.network;

import com.iwish.iwishclient.model.FriendRequest;
import com.iwish.iwishclient.model.User;

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
}
