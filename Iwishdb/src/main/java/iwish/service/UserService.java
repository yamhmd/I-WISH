package iwish.service;

import iwish.db.UserDAO;
import iwish.model.User;
import iwish.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

public class UserService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserDAO userDAO = new UserDAO();

    // REGISTER
    public int register(String username, String email, String password) throws SQLException {
        username = (username == null) ? "" : username.trim();
        email = (email == null) ? "" : email.trim();

        if (username.isEmpty()) {
            throw new IllegalStateException("Username is required");
        }
        if (username.length() > 50) {
            throw new IllegalStateException("Username is too long (max 50 characters)");
        }
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            throw new IllegalStateException("Invalid email");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalStateException("Password must be at least 6 characters");
        }

        if (userDAO.usernameExists(username)) {
            throw new IllegalStateException("Username already taken");
        }
        if (userDAO.emailExists(email)) {
            throw new IllegalStateException("Email already registered");
        }
        String passwordHash = PasswordUtil.hash(password);
        return userDAO.createUser(username, email, passwordHash);
    }

    // LOGIN
    public Map<String, Object> login(String username, String password) throws SQLException {
        if (username == null || password == null) {
            throw new IllegalStateException("Invalid credentials");
        }
        User user = userDAO.findByUsername(username.trim());
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new IllegalStateException("Invalid credentials");
        }
        return Map.of("user_id", user.getUserId(), "username", user.getUsername());
    }
}