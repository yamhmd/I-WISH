package iwish.logic;

import iwish.net.Request;
import iwish.net.Response;
import iwish.server.Session;

/**
 * THE HANDOVER POINT BETWEEN PERSON 2 AND PERSON 3.
 *
 * One method per action in PROTOCOL.md. Person 2's dispatch layer already
 * routes every action to the matching method here; Person 3 implements them
 * one at a time (TASK_ASSIGNMENTS Person 3 item 9 -- "hand each finished
 * method to Person 2 ... one action at a time"). Because the seam is an
 * interface, that handover is literally "delete the stub method, write the
 * real one" -- no networking code changes when logic lands.
 *
 * What Person 2's layer has already done before a method here is called:
 *   - the line was valid JSON and had an "action" field
 *   - the session check passed (see Session / Dispatcher): for every action
 *     except REGISTER, LOGIN and VIEW_CATALOG the socket is logged in and
 *     the request's user_id matches the session's
 *
 * What Person 3's method is responsible for:
 *   - reading its own fields via request.getInt(...) / getString(...) etc.
 *     (these throw ProtocolException on a missing field and the caller turns
 *     that into the standard ERROR response)
 *   - the authorisation rules that need the database, e.g. "Not friends" on
 *     VIEW_FRIEND_WISHLIST and "Cannot contribute to your own wish item"
 *   - returning Response.ok(...) or Response.error(...) with one of the
 *     agreed messages in iwish.net.Protocol.Errors
 *
 * Exceptions are also fine: an IllegalStateException thrown by the DAO layer
 * is converted into ERROR with its own message, and a SQLException becomes
 * "Internal iwish.server error" with a stack trace in the iwish.server log.
 */
public interface BusinessLogic {

    Response register(Request request, Session session) throws Exception;              // PROTOCOL #1
    Response login(Request request, Session session) throws Exception;                 // #2
    Response addFriend(Request request, Session session) throws Exception;             // #3
    Response acceptFriend(Request request, Session session) throws Exception;          // #4
    Response declineFriend(Request request, Session session) throws Exception;         // #5
    Response removeFriend(Request request, Session session) throws Exception;          // #6
    Response viewFriends(Request request, Session session) throws Exception;           // #7
    Response viewPendingRequests(Request request, Session session) throws Exception;   // #8
    Response viewCatalog(Request request, Session session) throws Exception;           // #9
    Response createWishItem(Request request, Session session) throws Exception;        // #10
    Response updateWishItem(Request request, Session session) throws Exception;        // #11
    Response deleteWishItem(Request request, Session session) throws Exception;        // #12
    Response viewFriendWishlist(Request request, Session session) throws Exception;    // #13
    Response contribute(Request request, Session session) throws Exception;            // #14
    Response getNotifications(Request request, Session session) throws Exception;      // #15
    Response markNotificationRead(Request request, Session session) throws Exception;  // #16
}
