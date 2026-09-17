package iwish.db;

import iwish.model.Friend;
import iwish.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * All queries touching the Friends table.
 *
 * IMPORTANT MODELING NOTE: a friendship is stored as ONE row, created by
 * whoever sent the request (user_id = sender, friend_user_id = receiver,
 * status = 'pending'). When accepted, that SAME row flips to 'accepted' —
 * we do not create a second mirrored row. That means "is A friends with B"
 * and "get A's friends" both have to check BOTH directions (A as sender
 * OR A as receiver) wherever accepted friends are queried. Every method
 * below that deals with accepted friendships does this with an OR clause —
 * keep that pattern if you add new friend-related queries.
 */
public class FriendDAO {

    /**
     * Sends a friend request from senderId to receiverUsername.
     * Throws IllegalStateException with a clear reason if the request
     * can't be sent (already friends, already pending, user not found,
     * or sending to yourself) — Person 3 should catch this and translate
     * it into the matching PROTOCOL.md error message.
     */
    public void sendRequest(int senderId, String receiverUsername) throws SQLException {
        UserDAO userDAO = new UserDAO();
        User receiver = userDAO.findByUsername(receiverUsername);
        if (receiver == null) {
            throw new IllegalStateException("User not found");
        }
        if (receiver.getUserId() == senderId) {
            throw new IllegalStateException("Cannot add yourself as a friend");
        }
        if (areFriends(senderId, receiver.getUserId())) {
            throw new IllegalStateException("Already friends");
        }
        if (hasPendingRequestBetween(senderId, receiver.getUserId())) {
            throw new IllegalStateException("Friend request already pending");
        }

        String sql = "INSERT INTO Friends (user_id, friend_user_id, status) VALUES (?, ?, 'pending')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiver.getUserId());
            ps.executeUpdate();
        }
    }

    /**
     * Accepts a pending request. receiverId must be the person who
     * RECEIVED the request (i.e. friend_user_id in the row) — this is
     * enforced in the WHERE clause so one user can't accept a request
     * that wasn't sent to them.
     *
     * @return true if a pending request was found and accepted, false if not
     */
    public boolean acceptRequest(int receiverId, int requesterId) throws SQLException {
        String sql = "UPDATE Friends SET status = 'accepted' " +
                     "WHERE user_id = ? AND friend_user_id = ? AND status = 'pending'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requesterId);
            ps.setInt(2, receiverId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Declines (deletes) a pending request. Same direction check as accept.
     *
     * @return true if a pending request was found and removed, false if not
     */
    public boolean declineRequest(int receiverId, int requesterId) throws SQLException {
        String sql = "DELETE FROM Friends " +
                     "WHERE user_id = ? AND friend_user_id = ? AND status = 'pending'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requesterId);
            ps.setInt(2, receiverId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Removes an existing ACCEPTED friendship, regardless of which side
     * originally sent the request.
     *
     * @return true if a friendship was found and removed, false if not
     */
    public boolean removeFriend(int userId, int friendId) throws SQLException {
        String sql = "DELETE FROM Friends " +
                     "WHERE status = 'accepted' AND " +
                     "((user_id = ? AND friend_user_id = ?) OR (user_id = ? AND friend_user_id = ?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.setInt(3, friendId);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /** All accepted friends of a user, as User objects (checks both directions). */
    public List<User> getFriends(int userId) throws SQLException {
        String sql =
            "SELECT u.* FROM Users u " +
            "JOIN Friends f ON " +
            "  ((f.user_id = ? AND f.friend_user_id = u.user_id) OR " +
            "   (f.friend_user_id = ? AND f.user_id = u.user_id)) " +
            "WHERE f.status = 'accepted'";

        List<User> friends = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                UserDAO userDAO = new UserDAO(); // reuse row-mapping logic indirectly
                while (rs.next()) {
                    friends.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getTimestamp("created_at")
                    ));
                }
            }
        }
        return friends;
    }

    /** Incoming pending requests for this user (i.e. this user is friend_user_id), with sender's username attached. */
    public List<Friend> getPendingRequests(int userId) throws SQLException {
        String sql =
            "SELECT f.*, u.username AS sender_username FROM Friends f " +
            "JOIN Users u ON u.user_id = f.user_id " +
            "WHERE f.friend_user_id = ? AND f.status = 'pending'";

        List<Friend> requests = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Friend f = new Friend(
                        rs.getInt("friend_id"),
                        rs.getInt("user_id"),
                        rs.getInt("friend_user_id"),
                        rs.getString("status"),
                        rs.getTimestamp("requested_at")
                    );
                    f.setOtherUsername(rs.getString("sender_username"));
                    requests.add(f);
                }
            }
        }
        return requests;
    }

    /** True if userId and otherId are ACCEPTED friends (checks both directions). */
    public boolean areFriends(int userId, int otherId) throws SQLException {
        String sql =
            "SELECT 1 FROM Friends WHERE status = 'accepted' AND " +
            "((user_id = ? AND friend_user_id = ?) OR (user_id = ? AND friend_user_id = ?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, otherId);
            ps.setInt(3, otherId);
            ps.setInt(4, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasPendingRequestBetween(int userId, int otherId) throws SQLException {
        String sql =
            "SELECT 1 FROM Friends WHERE status = 'pending' AND " +
            "((user_id = ? AND friend_user_id = ?) OR (user_id = ? AND friend_user_id = ?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, otherId);
            ps.setInt(3, otherId);
            ps.setInt(4, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
