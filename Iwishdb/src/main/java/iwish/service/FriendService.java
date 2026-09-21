package iwish.service;

import iwish.db.FriendDAO;
import iwish.model.Friend;
import iwish.model.User;
import iwish.net.Protocol;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FriendService {

    private final FriendDAO friendDAO = new FriendDAO();

    // ADD_FRIEND
    public void addFriend(int userId, String friendUsername) throws SQLException {
        friendDAO.sendRequest(userId, friendUsername);
    }

    // ACCEPT_FRIEND
    public void acceptFriend(int userId, int requesterId) throws SQLException {
        boolean accepted = friendDAO.acceptRequest(userId, requesterId);
        if (!accepted) {
            throw new IllegalStateException("Friend request not found");
        }
    }

    // DECLINE_FRIEND
    public void declineFriend(int userId, int requesterId) throws SQLException {
        boolean declined = friendDAO.declineRequest(userId, requesterId);
        if (!declined) {
            throw new IllegalStateException("Friend request not found");
        }
    }

    // REMOVE_FRIEND
    public void removeFriend(int userId, int friendId) throws SQLException {
        boolean removed = friendDAO.removeFriend(userId, friendId);
        if (!removed) {
            throw new IllegalStateException("Not friends");
        }
    }

    // VIEW_FRIENDS
    public List<Map<String, Object>> viewFriends(int userId) throws SQLException {
        List<User> friends = friendDAO.getFriends(userId);
        return friends.stream()
                .map(f -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("user_id", f.getUserId());
                    m.put("username", f.getUsername());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // VIEW_PENDING_REQUESTS
    public List<Map<String, Object>> viewPendingRequests(int userId) throws SQLException {
        List<Friend> pending = friendDAO.getPendingRequests(userId);
        return pending.stream()
                .map(f -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("requester_id", f.getUserId());
                    m.put("username", f.getOtherUsername());
                    m.put("requested_at", Protocol.iso8601(f.getRequestedAt()));
                    return m;
                })
                .collect(Collectors.toList());
    }
}
