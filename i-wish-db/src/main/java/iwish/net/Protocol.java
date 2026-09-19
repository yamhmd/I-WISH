package iwish.net;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * PROTOCOL.md expressed as constants, so nobody has to retype a string and
 * get it subtly wrong. Person 2 owns this file's accuracy along with
 * PROTOCOL.md itself -- if one changes, change the other in the same commit.
 */
public final class Protocol {

    private Protocol() { }

    // ---- field names shared by every message -------------------------
    public static final String ACTION = "action";
    public static final String STATUS = "status";
    public static final String MESSAGE = "message";
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";

    // ---- the 16 actions, in PROTOCOL.md order ------------------------
    public static final String REGISTER               = "REGISTER";
    public static final String LOGIN                  = "LOGIN";
    public static final String ADD_FRIEND             = "ADD_FRIEND";
    public static final String ACCEPT_FRIEND          = "ACCEPT_FRIEND";
    public static final String DECLINE_FRIEND         = "DECLINE_FRIEND";
    public static final String REMOVE_FRIEND          = "REMOVE_FRIEND";
    public static final String VIEW_FRIENDS           = "VIEW_FRIENDS";
    public static final String VIEW_PENDING_REQUESTS  = "VIEW_PENDING_REQUESTS";
    public static final String VIEW_CATALOG           = "VIEW_CATALOG";
    public static final String CREATE_WISH_ITEM       = "CREATE_WISH_ITEM";
    public static final String UPDATE_WISH_ITEM       = "UPDATE_WISH_ITEM";
    public static final String DELETE_WISH_ITEM       = "DELETE_WISH_ITEM";
    public static final String VIEW_FRIEND_WISHLIST   = "VIEW_FRIEND_WISHLIST";
    public static final String CONTRIBUTE             = "CONTRIBUTE";
    public static final String GET_NOTIFICATIONS      = "GET_NOTIFICATIONS";
    public static final String MARK_NOTIFICATION_READ = "MARK_NOTIFICATION_READ";

    /**
     * The agreed error strings. The DAO layer already throws
     * IllegalStateException carrying several of these exact words
     * ("User not found", "Already friends", "Item already in wish list",
     * "Cannot delete an item with existing contributions", ...), which is why
     * Dispatcher can pass an IllegalStateException message straight through.
     */
    public static final class Errors {
        private Errors() { }
        public static final String INVALID_CREDENTIALS   = "Invalid credentials";
        public static final String USERNAME_TAKEN        = "Username already taken";
        public static final String EMAIL_TAKEN           = "Email already registered";
        public static final String USER_NOT_FOUND        = "User not found";
        public static final String ALREADY_FRIENDS       = "Already friends";
        public static final String REQUEST_PENDING       = "Friend request already pending";
        public static final String NOT_FRIENDS           = "Not friends";
        public static final String ITEM_NOT_FOUND        = "Item not found";
        public static final String ITEM_ALREADY_IN_LIST  = "Item already in wish list";
        public static final String CANNOT_EDIT           = "Cannot edit an item with existing contributions";
        public static final String CANNOT_DELETE         = "Cannot delete an item with existing contributions";
        public static final String AMOUNT_EXCEEDS        = "Amount exceeds remaining price";
        public static final String OWN_ITEM              = "Cannot contribute to your own wish item";
        public static final String ITEM_COMPLETE         = "Item already complete";
        public static final String UNAUTHORIZED          = "Unauthorized";
        public static final String NOT_LOGGED_IN         = "Unauthorized";
        public static final String MALFORMED_JSON        = "Malformed request: not valid JSON";
        public static final String MISSING_ACTION        = "Missing 'action' field";
        public static final String INTERNAL              = "Internal iwish.server error";
    }

    private static final DateTimeFormatter ISO =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    /** Formats a SQL timestamp as the ISO-8601 string the protocol requires, e.g. 2026-09-17T14:30:00Z. */
    public static String iso8601(Timestamp ts) {
        return ts == null ? null : ISO.format(ts.toInstant());
    }
}
