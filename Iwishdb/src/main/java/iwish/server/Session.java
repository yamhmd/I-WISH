package iwish.server;

/**
 * State that belongs to one socket, for the life of that socket.
 *
 * PROTOCOL.md, General Rules: "Once a client logs in, the iwish.server should
 * associate user_id with that socket/session so later requests don't need to
 * keep re-sending it insecurely -- but for simplicity in this project, we still
 * include user_id explicitly in every request so the iwish.server can validate it
 * against the session."
 *
 * That is exactly what this class is for: login() records who this socket is,
 * and Dispatcher compares the user_id in each later request against it. A
 * mismatch is "Unauthorized" -- client A cannot act as client B by editing a
 * number in the JSON.
 *
 * One Session per ClientHandler thread, never shared, so no synchronisation
 * is needed.
 */
public class Session {

    private static final int NOT_LOGGED_IN = -1;

    private final String connectionId;
    private int userId = NOT_LOGGED_IN;
    private String username;

    public Session(String connectionId) {
        this.connectionId = connectionId;
    }

    public String connectionId() { return connectionId; }

    public boolean isLoggedIn() { return userId != NOT_LOGGED_IN; }

    /** Throws if not logged in -- call isLoggedIn() first, or let Dispatcher's guard run. */
    public int userId() {
        if (!isLoggedIn()) {
            throw new IllegalStateException("Session is not logged in");
        }
        return userId;
    }

    public String username() { return username; }

    /** Called by Dispatcher after REGISTER or LOGIN succeeds. */
    public void login(int userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    /** Clears the session (used on logout or when re-authenticating on the same socket). */
    public void logout() {
        this.userId = NOT_LOGGED_IN;
        this.username = null;
    }

    @Override
    public String toString() {
        return isLoggedIn() ? connectionId + "/" + username + "#" + userId : connectionId + "/anonymous";
    }
}
