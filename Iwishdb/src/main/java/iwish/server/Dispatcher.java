package iwish.server;

import iwish.logic.BusinessLogic;
import iwish.net.Protocol;
import iwish.net.Request;
import iwish.net.Response;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * TASK_ASSIGNMENTS, Person 2 item 5: read the incoming JSON, pull out
 * "action", route it to a handler.
 *
 * Every action in PROTOCOL.md is registered in the table below, each pointing
 * at a method of the BusinessLogic interface. Swapping the stub for Person 3's
 * real implementation (item 8) is a one-line change in IWishServer -- this
 * class never changes.
 *
 * Three things happen around every call, so no handler has to repeat them:
 *   1. Unknown action  -> standard ERROR, thread stays alive.
 *   2. Session guard   -> everything except REGISTER, LOGIN and VIEW_CATALOG
 *                         requires a logged-in socket whose user_id matches
 *                         the user_id in the request, else "Unauthorized".
 *   3. Exception map   -> ProtocolException (bad/missing field) and
 *                         IllegalStateException (thrown by the DAO layer with
 *                         a protocol-worded message) become clean ERROR
 *                         responses; SQLException and anything unexpected
 *                         become "Internal iwish.server error" and are logged.
 */
public class Dispatcher {

    /** What a registered action does. */
    @FunctionalInterface
    private interface Handler {
        Response handle(Request request, Session session) throws Exception;
    }

    /** Actions a client may send before logging in. */
    private static final Set<String> PUBLIC_ACTIONS = Set.of(
        Protocol.REGISTER, Protocol.LOGIN, Protocol.VIEW_CATALOG
    );

    private final Map<String, Handler> routes = new LinkedHashMap<>();
    private final BusinessLogic logic;

    public Dispatcher(BusinessLogic logic) {
        this.logic = logic;

        routes.put(Protocol.REGISTER,               logic::register);
        routes.put(Protocol.LOGIN,                  logic::login);
        routes.put(Protocol.ADD_FRIEND,             logic::addFriend);
        routes.put(Protocol.ACCEPT_FRIEND,          logic::acceptFriend);
        routes.put(Protocol.DECLINE_FRIEND,         logic::declineFriend);
        routes.put(Protocol.REMOVE_FRIEND,          logic::removeFriend);
        routes.put(Protocol.VIEW_FRIENDS,           logic::viewFriends);
        routes.put(Protocol.VIEW_PENDING_REQUESTS,  logic::viewPendingRequests);
        routes.put(Protocol.VIEW_CATALOG,           logic::viewCatalog);
        routes.put(Protocol.CREATE_WISH_ITEM,       logic::createWishItem);
        routes.put(Protocol.UPDATE_WISH_ITEM,       logic::updateWishItem);
        routes.put(Protocol.DELETE_WISH_ITEM,       logic::deleteWishItem);
        routes.put(Protocol.VIEW_MY_WISHLIST,       logic::viewMyWishlist);
        routes.put(Protocol.VIEW_FRIEND_WISHLIST,   logic::viewFriendWishlist);
        routes.put(Protocol.CONTRIBUTE,             logic::contribute);
        routes.put(Protocol.GET_NOTIFICATIONS,      logic::getNotifications);
        routes.put(Protocol.MARK_NOTIFICATION_READ, logic::markNotificationRead);
    }

    /** The action names this iwish.server actually answers -- used by the self-check on startup. */
    public Set<String> supportedActions() {
        return routes.keySet();
    }

    public Response dispatch(Request request, Session session) {
        String action = request.action();

        if (action == null) {
            return Response.error(Protocol.Errors.MISSING_ACTION);
        }

        Handler handler = routes.get(action);
        if (handler == null) {
            return Response.error("Unknown action: " + action);
        }

        Response guardFailure = checkSession(action, request, session);
        if (guardFailure != null) {
            return guardFailure;
        }

        try {
            Response response = handler.handle(request, session);
            if (response == null) {
                ServerLog.error(session + " handler for " + action + " returned null");
                return Response.error(Protocol.Errors.INTERNAL);
            }
            return response;

        } catch (Request.ProtocolException e) {
            // Missing or wrong-typed field -- the client's fault, tell it plainly.
            return Response.error(e.getMessage());

        } catch (IllegalStateException e) {
            // The DAO layer signals its business rules this way, with messages
            // already worded to match PROTOCOL.md ("User not found",
            // "Already friends", "Cannot delete an item with existing
            // contributions", ...), so pass the message straight through.
            return Response.error(e.getMessage());

        } catch (SQLException e) {
            ServerLog.error(session + " database error on " + action + ": " + e.getMessage());
            return Response.error(Protocol.Errors.INTERNAL);

        } catch (Exception e) {
            // Nothing a handler throws is allowed to kill the thread.
            ServerLog.error(session + " unhandled error on " + action + ": " + e);
            e.printStackTrace();
            return Response.error(Protocol.Errors.INTERNAL);
        }
    }

    /**
     * Enforces the session rule from PROTOCOL.md's General Rules. Returns null
     * when the request may proceed, or the ERROR response to send back.
     */
    private Response checkSession(String action, Request request, Session session) {
        if (PUBLIC_ACTIONS.contains(action)) {
            return null;
        }
        if (!session.isLoggedIn()) {
            return Response.error(Protocol.Errors.UNAUTHORIZED);
        }
        try {
            int claimedUserId = request.getInt("user_id");
            if (claimedUserId != session.userId()) {
                ServerLog.warn(session + " sent user_id=" + claimedUserId + " on " + action + " -- rejected");
                return Response.error(Protocol.Errors.UNAUTHORIZED);
            }
        } catch (Request.ProtocolException e) {
            return Response.error(e.getMessage());
        }
        return null;
    }

    /**
     * Binds the socket to a user after a successful REGISTER or LOGIN.
     * Called by ClientHandler once the response is known to be OK, so the
     * rule lives in the networking layer where connection state belongs.
     */
    void bindSessionIfAuthenticated(String action, Response response, Session session) {
        if (!response.isOk()) {
            return;
        }
        if (!Protocol.REGISTER.equals(action) && !Protocol.LOGIN.equals(action)) {
            return;
        }
        Object userId = response.get("user_id");
        Object username = response.get("username");
        if (userId instanceof Number n) {
            session.login(n.intValue(), username == null ? null : username.toString());
            ServerLog.info(session + " authenticated via " + action);
        }
    }

    BusinessLogic logic() {
        return logic;
    }
}
