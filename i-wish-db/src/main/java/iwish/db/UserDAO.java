package iwish.db;

import iwish.model.User;

import java.sql.*;

/**
 * All queries touching the Users table.
 *
 * SECURITY NOTE: every method here uses PreparedStatement with ? placeholders,
 * never string concatenation of user input into SQL. Do not "simplify" this
 * to make queries shorter — string-concatenated SQL is how SQL injection
 * happens, and it's an easy, avoidable mistake under time pressure.
 */
public class UserDAO {

    /**
     * Creates a new user. Caller (business logic layer) is responsible for
     * hashing the password with PasswordUtil.hash() BEFORE calling this —
     * this method just stores whatever string it's given as password_hash.
     *
     * @return the generated user_id
     * @throws SQLException if username or email already exists (unique constraint violation)
     */
    public int createUser(String username, String email, String passwordHash) throws SQLException {
        String sql = "INSERT INTO Users (username, email, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("User insert did not return a generated key");
        }
    }

    /** Returns null if no user with that username exists. */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    /** Returns null if no user with that id exists. */
    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM Users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        return findByUsername(username) != null;
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM Users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getTimestamp("created_at")
        );
    }
}
