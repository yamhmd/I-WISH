package iwish.test;

import iwish.logic.StubBusinessLogic;
import iwish.server.IWishServer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static iwish.test.ProtocolClient.fields;

/**
 * Run this class directly to verify the iwish.server against PROTOCOL.md and
 * against Person 2's task list -- the networking equivalent of Person 1's
 * DBTestMain.
 *
 *     java -cp out iwish.iwish.test.ProtocolVerifier
 *
 * It starts a real iwish.server on a free port with the stub business logic, opens
 * real TCP sockets, and checks:
 *
 *   A. all protocol actions exist and answer with the fields PROTOCOL.md promises
 *   B. the general rules (status field, error shape, newline framing,
 *      ISO-8601 timestamps, 2-decimal money, passwords never echoed)
 *   C. malformed input handling -- bad JSON, no action, unknown action,
 *      wrong-typed field -- never crashes the iwish.server or the connection
 *   D. the session rule (user_id validated against the logged-in socket)
 *   E. multi-client concurrency and clean connection lifecycle (no leaks)
 *
 * Exit code is 0 when everything passes, 1 otherwise, so it can be wired
 * into a CI check later if the team wants one.
 *
 * It needs NO database and NO MySQL driver -- nothing in the DB layer is
 * touched or changed by any of this.
 */
public class ProtocolVerifier {

    private static final Pattern ISO_8601 = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
    private static final Pattern TWO_DECIMALS = Pattern.compile("-?\\d+\\.\\d{2}");

    private static final List<String> failures = new ArrayList<>();
    private static int checks = 0;

    private static final String HOST = "127.0.0.1";
    private static final String PASSWORD = "password123";

    public static void main(String[] args) throws Exception {
        IWishServer server = new IWishServer(0, new StubBusinessLogic()); // port 0 = any free port
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "iwish-iwish.server-under-iwish.test");
        serverThread.setDaemon(true);
        serverThread.start();

        waitUntilListening(server);
        int port = server.actualPort();
        System.out.println("\n================ PROTOCOL VERIFICATION (port " + port + ") ================\n");

        try {
            sectionA(port);
            sectionB(port);
            sectionC(port);
            sectionD(port);
            sectionE(port);
        } finally {
            server.stop();
        }

        // Lifecycle: after stop(), nothing should still be tracked.
        check("Lifecycle: no connections left tracked after shutdown", server.openConnectionCount() == 0,
            "still tracking " + server.openConnectionCount());

        report();
    }

    // ==================================================================
    // A. every action, every documented field
    // ==================================================================

    private static void sectionA(int port) throws Exception {
        section("A. Protocol actions (PROTOCOL.md #1-#16)");

        try (ProtocolClient c = new ProtocolClient(HOST, port)) {

            // #1 REGISTER
            Map<String, Object> reg = c.send("REGISTER",
                fields("username", "ahmed", "email", "ahmed@mail.com", "password", PASSWORD));
            checkOk("#1 REGISTER", reg);
            checkFields("#1 REGISTER", reg, "user_id", "username");

            int userId = intOf(reg.get("user_id"));

            // #9 VIEW_CATALOG (no user_id in the request, per the spec)
            Map<String, Object> catalog = c.send("VIEW_CATALOG");
            checkOk("#9 VIEW_CATALOG", catalog);
            checkFields("#9 VIEW_CATALOG", catalog, "items");
            checkListOfObjects("#9 VIEW_CATALOG items[]", catalog.get("items"), "item_id", "name", "price");

            // #3 ADD_FRIEND
            checkOkWithMessage("#3 ADD_FRIEND",
                c.send("ADD_FRIEND", fields("user_id", userId, "friend_username", "mohamed")));

            // #4 ACCEPT_FRIEND
            checkOkWithMessage("#4 ACCEPT_FRIEND",
                c.send("ACCEPT_FRIEND", fields("user_id", userId, "requester_id", 1)));

            // #5 DECLINE_FRIEND
            checkOkWithMessage("#5 DECLINE_FRIEND",
                c.send("DECLINE_FRIEND", fields("user_id", userId, "requester_id", 1)));

            // #6 REMOVE_FRIEND
            checkOkWithMessage("#6 REMOVE_FRIEND",
                c.send("REMOVE_FRIEND", fields("user_id", userId, "friend_id", 2)));

            // #7 VIEW_FRIENDS
            Map<String, Object> friends = c.send("VIEW_FRIENDS", fields("user_id", userId));
            checkOk("#7 VIEW_FRIENDS", friends);
            checkListOfObjects("#7 VIEW_FRIENDS friends[]", friends.get("friends"), "user_id", "username");

            // #8 VIEW_PENDING_REQUESTS
            Map<String, Object> pending = c.send("VIEW_PENDING_REQUESTS", fields("user_id", userId));
            checkOk("#8 VIEW_PENDING_REQUESTS", pending);
            checkListOfObjects("#8 VIEW_PENDING_REQUESTS requests[]", pending.get("requests"),
                "requester_id", "username", "requested_at");

            // #10 CREATE_WISH_ITEM
            Map<String, Object> created = c.send("CREATE_WISH_ITEM", fields("user_id", userId, "item_id", 10));
            checkOk("#10 CREATE_WISH_ITEM", created);
            checkFields("#10 CREATE_WISH_ITEM", created, "wish_id", "item_id", "amount_raised", "is_complete");
            check("#10 CREATE_WISH_ITEM is_complete is a boolean",
                created.get("is_complete") instanceof Boolean, "got " + created.get("is_complete"));

            // #11 UPDATE_WISH_ITEM
            checkOkWithMessage("#11 UPDATE_WISH_ITEM",
                c.send("UPDATE_WISH_ITEM", fields("user_id", userId, "wish_id", 55, "item_id", 12)));

            // #12 DELETE_WISH_ITEM
            checkOkWithMessage("#12 DELETE_WISH_ITEM",
                c.send("DELETE_WISH_ITEM", fields("user_id", userId, "wish_id", 55)));

            // #12 error path -- the contributions edge case
            checkError("#12 DELETE_WISH_ITEM with contributions",
                c.send("DELETE_WISH_ITEM", fields("user_id", userId, "wish_id", 99)),
                "Cannot delete an item with existing contributions");

            // #13 VIEW_MY_WISHLIST
            Map<String, Object> myWishlist = c.send("VIEW_MY_WISHLIST", fields("user_id", userId));
            checkOk("#13 VIEW_MY_WISHLIST", myWishlist);
            checkFields("#13 VIEW_MY_WISHLIST", myWishlist, "wish_items");
            checkListOfObjects("#13 VIEW_MY_WISHLIST wish_items[]", myWishlist.get("wish_items"),
                "wish_id", "item_id", "name", "price", "amount_raised", "is_complete");

            // #14 VIEW_FRIEND_WISHLIST
            Map<String, Object> wishlist = c.send("VIEW_FRIEND_WISHLIST", fields("user_id", userId, "friend_id", 2));
            checkOk("#14 VIEW_FRIEND_WISHLIST", wishlist);
            checkFields("#14 VIEW_FRIEND_WISHLIST", wishlist, "friend_username", "wish_items");
            checkListOfObjects("#14 VIEW_FRIEND_WISHLIST wish_items[]", wishlist.get("wish_items"),
                "wish_id", "item_id", "name", "price", "amount_raised", "is_complete");

            // #14 error path
            checkError("#14 VIEW_FRIEND_WISHLIST when not friends",
                c.send("VIEW_FRIEND_WISHLIST", fields("user_id", userId, "friend_id", 999)), "Not friends");

            // #15 CONTRIBUTE
            Map<String, Object> contribution = c.send("CONTRIBUTE",
                fields("user_id", userId, "wish_id", 60, "amount", new BigDecimal("300.00")));
            checkOk("#15 CONTRIBUTE", contribution);
            checkFields("#15 CONTRIBUTE", contribution, "wish_completed", "message");
            check("#15 CONTRIBUTE wish_completed is a boolean",
                contribution.get("wish_completed") instanceof Boolean, "got " + contribution.get("wish_completed"));

            // #15 error path
            checkError("#15 CONTRIBUTE over the remaining price",
                c.send("CONTRIBUTE", fields("user_id", userId, "wish_id", 60, "amount", new BigDecimal("900.00"))),
                "Amount exceeds remaining price");

            // #16 GET_NOTIFICATIONS
            Map<String, Object> notifications = c.send("GET_NOTIFICATIONS", fields("user_id", userId));
            checkOk("#16 GET_NOTIFICATIONS", notifications);
            checkListOfObjects("#16 GET_NOTIFICATIONS notifications[]", notifications.get("notifications"),
                "notif_id", "type", "message", "is_read", "created_at");

            // #17 MARK_NOTIFICATION_READ
            checkOkWithMessage("#17 MARK_NOTIFICATION_READ",
                c.send("MARK_NOTIFICATION_READ", fields("user_id", userId, "notif_id", 200)));
        }

        // #2 LOGIN, on its own connection
        try (ProtocolClient c = new ProtocolClient(HOST, port)) {
            Map<String, Object> login = c.send("LOGIN", fields("username", "ahmed", "password", PASSWORD));
            checkOk("#2 LOGIN with the correct password", login);
            checkFields("#2 LOGIN", login, "user_id", "username");
        }
        try (ProtocolClient c = new ProtocolClient(HOST, port)) {
            checkError("#2 LOGIN with the wrong password",
                c.send("LOGIN", fields("username", "ahmed", "password", "wrongpass")), "Invalid credentials");
        }
    }

    // ==================================================================
    // B. the general rules at the top of PROTOCOL.md
    // ==================================================================

    private static void sectionB(int port) throws Exception {
        section("B. General rules");

        try (ProtocolClient c = new ProtocolClient(HOST, port)) {
            String raw = c.sendRaw("{\"action\":\"VIEW_CATALOG\"}");

            check("Every response is a single line (newline framing)",
                !raw.contains("\n"), "reply contained a newline");
            check("Every response carries a status field",
                raw.contains("\"status\""), raw);
            check("Money is written with two decimal places",
                TWO_DECIMALS.matcher(raw).find(), raw);

            Map<String, Object> reg = c.send("REGISTER",
                fields("username", "verify_user", "email", "v@mail.com", "password", PASSWORD));
            int userId = intOf(reg.get("user_id"));

            String rawPending = c.sendRaw("{\"action\":\"VIEW_PENDING_REQUESTS\",\"user_id\":" + userId + "}");
            check("Timestamps are ISO-8601 (e.g. 2026-09-17T10:00:00Z)",
                ISO_8601.matcher(rawPending).find(), rawPending);

            String rawNotifs = c.sendRaw("{\"action\":\"GET_NOTIFICATIONS\",\"user_id\":" + userId + "}");
            check("Notification timestamps are ISO-8601",
                ISO_8601.matcher(rawNotifs).find(), rawNotifs);

            // "Passwords are never sent back in any response, ever."
            String rawLoginish = c.sendRaw("{\"action\":\"REGISTER\",\"username\":\"pw_probe\","
                + "\"email\":\"p@mail.com\",\"password\":\"" + PASSWORD + "\"}");
            check("Passwords are never echoed in a response",
                !rawLoginish.contains(PASSWORD) && !rawLoginish.contains("password"), rawLoginish);
        }
    }

    // ==================================================================
    // C. malformed input (Person 2, task 11)
    // ==================================================================

    private static void sectionC(int port) throws Exception {
        section("C. Malformed-request handling");

        try (ProtocolClient c = new ProtocolClient(HOST, port)) {
            checkErrorRaw("Bad JSON gets a standard ERROR", c.sendRaw("{this is not json"));
            checkErrorRaw("A bare string gets a standard ERROR", c.sendRaw("hello iwish.server"));
            checkErrorRaw("A JSON array gets a standard ERROR", c.sendRaw("[1,2,3]"));
            checkErrorRaw("Empty object (no action) gets a standard ERROR", c.sendRaw("{}"));
            checkErrorRaw("Unknown action gets a standard ERROR", c.sendRaw("{\"action\":\"LAUNCH_ROCKET\"}"));
            checkErrorRaw("Non-string action gets a standard ERROR", c.sendRaw("{\"action\":42}"));

            // The connection must still be alive after all of that.
            Map<String, Object> stillWorks = c.send("VIEW_CATALOG");
            checkOk("Connection still usable after malformed input", stillWorks);
        }

        try (ProtocolClient c = new ProtocolClient(HOST, port)) {
            Map<String, Object> reg = c.send("REGISTER",
                fields("username", "badfields", "email", "b@mail.com", "password", PASSWORD));
            int userId = intOf(reg.get("user_id"));

            checkErrorRaw("Missing required field gets a standard ERROR",
                c.sendRaw("{\"action\":\"CONTRIBUTE\",\"user_id\":" + userId + ",\"wish_id\":60}"));
            checkErrorRaw("Wrong-typed field gets a standard ERROR",
                c.sendRaw("{\"action\":\"CONTRIBUTE\",\"user_id\":" + userId
                    + ",\"wish_id\":60,\"amount\":\"lots\"}"));
            checkErrorRaw("Non-numeric id gets a standard ERROR",
                c.sendRaw("{\"action\":\"MARK_NOTIFICATION_READ\",\"user_id\":" + userId
                    + ",\"notif_id\":\"abc\"}"));

            checkOk("Connection still usable after bad fields", c.send("VIEW_CATALOG"));
        }
    }

    // ==================================================================
    // D. session rule
    // ==================================================================

    private static void sectionD(int port) throws Exception {
        section("D. Session / user_id validation");

        try (ProtocolClient c = new ProtocolClient(HOST, port)) {
            checkError("Acting before login is Unauthorized",
                c.send("VIEW_FRIENDS", fields("user_id", 1)), "Unauthorized");
            checkOk("VIEW_CATALOG is allowed before login", c.send("VIEW_CATALOG"));
        }

        try (ProtocolClient c = new ProtocolClient(HOST, port)) {
            Map<String, Object> login = c.send("LOGIN", fields("username", "session_user", "password", PASSWORD));
            int userId = intOf(login.get("user_id"));

            checkOk("Own user_id is accepted after login", c.send("VIEW_FRIENDS", fields("user_id", userId)));
            checkError("Someone else's user_id is rejected",
                c.send("VIEW_FRIENDS", fields("user_id", userId + 12345)), "Unauthorized");
            checkOk("Connection still usable after a rejected user_id",
                c.send("VIEW_FRIENDS", fields("user_id", userId)));
        }
    }

    // ==================================================================
    // E. concurrency and connection lifecycle (tasks 4, 10, 12)
    // ==================================================================

    private static void sectionE(int port) throws Exception {
        section("E. Multi-client concurrency and lifecycle");

        final int clients = 20;
        final int callsEach = 10;
        CountDownLatch ready = new CountDownLatch(clients);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(clients);
        AtomicInteger okResponses = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        List<String> problems = java.util.Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < clients; i++) {
            final int n = i;
            Thread t = new Thread(() -> {
                try (ProtocolClient c = new ProtocolClient(HOST, port)) {
                    ready.countDown();
                    go.await();

                    Map<String, Object> login = c.send("LOGIN",
                        fields("username", "load_user_" + n, "password", PASSWORD));
                    int userId = intOf(login.get("user_id"));

                    for (int k = 0; k < callsEach; k++) {
                        Map<String, Object> r = c.send("VIEW_FRIENDS", fields("user_id", userId));
                        if ("OK".equals(r.get("status"))) {
                            okResponses.incrementAndGet();
                        } else {
                            errors.incrementAndGet();
                            problems.add("client " + n + ": " + r);
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    problems.add("client " + n + ": " + e);
                } finally {
                    done.countDown();
                }
            }, "load-client-" + i);
            t.start();
        }

        ready.await(10, TimeUnit.SECONDS);
        go.countDown();                                  // release them all at once
        boolean finished = done.await(30, TimeUnit.SECONDS);

        check(clients + " simultaneous clients all finished", finished, "timed out waiting for clients");
        check("All " + (clients * callsEach) + " concurrent responses were OK",
            errors.get() == 0 && okResponses.get() == clients * callsEach,
            "ok=" + okResponses.get() + " errors=" + errors.get() + " " + problems);

        // Every client closed its socket; the iwish.server should let go of all of them.
        long deadline = System.currentTimeMillis() + 5000;
        IWishServerHolder.waitForZero(deadline);
        check("Server released every closed connection (no socket/thread leak)",
            IWishServerHolder.lastCount == 0, "still tracking " + IWishServerHolder.lastCount);
    }

    /** Small indirection so sectionE can read the live connection count without a field. */
    private static final class IWishServerHolder {
        static IWishServer server;
        static int lastCount = -1;

        static void waitForZero(long deadline) throws InterruptedException {
            while (System.currentTimeMillis() < deadline) {
                lastCount = server == null ? 0 : server.openConnectionCount();
                if (lastCount == 0) return;
                Thread.sleep(50);
            }
        }
    }

    // ==================================================================
    // plumbing
    // ==================================================================

    private static void waitUntilListening(IWishServer server) throws InterruptedException {
        IWishServerHolder.server = server;
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (server.isRunning() && server.actualPort() > 0) {
                Thread.sleep(100); // let the accept loop settle
                return;
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("Server did not start in time");
    }

    private static int intOf(Object value) {
        if (value instanceof Number n) return n.intValue();
        throw new IllegalStateException("Expected a number, got: " + value);
    }

    private static void section(String title) {
        System.out.println("\n--- " + title + " ---");
    }

    private static void check(String name, boolean condition, String detail) {
        checks++;
        if (condition) {
            System.out.println("  PASS  " + name);
        } else {
            System.out.println("  FAIL  " + name + "   [" + detail + "]");
            failures.add(name);
        }
    }

    private static void checkOk(String name, Map<String, Object> response) {
        check(name + " -> OK", "OK".equals(response.get("status")), String.valueOf(response));
    }

    private static void checkOkWithMessage(String name, Map<String, Object> response) {
        checkOk(name, response);
        check(name + " carries a message", response.get("message") instanceof String, String.valueOf(response));
    }

    private static void checkError(String name, Map<String, Object> response, String expectedMessage) {
        check(name + " -> ERROR \"" + expectedMessage + "\"",
            "ERROR".equals(response.get("status")) && expectedMessage.equals(response.get("message")),
            String.valueOf(response));
    }

    private static void checkErrorRaw(String name, String rawReply) {
        boolean ok = rawReply.contains("\"status\":\"ERROR\"") && rawReply.contains("\"message\"");
        check(name, ok, rawReply);
    }

    private static void checkFields(String name, Map<String, Object> response, String... required) {
        for (String field : required) {
            check(name + " includes \"" + field + "\"", response.get(field) != null, String.valueOf(response));
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkListOfObjects(String name, Object value, String... requiredFields) {
        if (!(value instanceof List<?> list)) {
            check(name + " is an array", false, String.valueOf(value));
            return;
        }
        check(name + " is a non-empty array", !list.isEmpty(), "empty");
        if (list.isEmpty()) return;

        Object first = list.get(0);
        if (!(first instanceof Map)) {
            check(name + " holds objects", false, String.valueOf(first));
            return;
        }
        Map<String, Object> row = (Map<String, Object>) first;
        for (String field : requiredFields) {
            check(name + " row includes \"" + field + "\"", row.containsKey(field), String.valueOf(row));
        }
    }

    private static void report() {
        System.out.println("\n================ RESULT ================");
        System.out.println("Checks run: " + checks + "   Failed: " + failures.size());
        if (failures.isEmpty()) {
            System.out.println("ALL CHECKS PASSED -- the iwish.server matches PROTOCOL.md.");
        } else {
            System.out.println("FAILURES:");
            failures.forEach(f -> System.out.println("  - " + f));
        }
        System.out.println("========================================");
        System.exit(failures.isEmpty() ? 0 : 1);
    }
}
