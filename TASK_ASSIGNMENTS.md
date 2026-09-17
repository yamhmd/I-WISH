# i-Wish Task Assignments — 6 People

Each person's list is in the order they should do it. Dependencies are marked so everyone knows what they're waiting on and what others are waiting on from them. Cross-reference `PROTOCOL.md` for exact request/response shapes.

---

## Person 1 — Database & Connection Layer

**Owns:** schema, seed data, JDBC connection, CRUD queries, GitHub merge review.

1. Attend kickoff, confirm final table list with the group.
2. Write the SQL schema for: Users, Friends, CatalogItems, WishItems, Contributions, Notifications (with foreign keys).
3. Create the actual database instance and run the schema against it.
4. Seed 5–10 CatalogItems (name, price).
5. Seed 2–3 test Users for development/demo.
6. Push schema + seed script to GitHub immediately — this is the first thing that unblocks Person 3.
7. Write the JDBC Connection class the server will use.
8. Write CRUD queries for Users (insert on register, select on login).
9. Write CRUD queries for Friends (insert request, update status, select friends/pending).
10. Write CRUD queries for WishItems (insert, update, delete, select — including the friend's-wishlist select).
11. Write CRUD queries for CatalogItems (select all, for VIEW_CATALOG).
12. Write the **guarded atomic update** for Contributions — `UPDATE WishItems SET amount_raised = amount_raised + ? WHERE wish_id = ? AND amount_raised + ? <= price` — plus the insert into Contributions itself. This is the most important query in the project; test it in isolation before handing it to Person 3.
13. Write CRUD queries for Notifications (insert, select unread/all, mark read).
14. Export a fresh DB backup once schema is stable, hand to Person 3 so they can develop against real data.
15. From here on: review and merge every pull request as it lands, keep main branch always buildable.
16. Near deadline: export the final database backup for delivery.

---

## Person 2 — Server Networking & Protocol

**Owns:** socket server, threading, `PROTOCOL.md`, connection lifecycle.

1. Attend kickoff, drive the group agreement on the protocol action list.
2. Create and push `PROTOCOL.md` (already drafted — confirm it with the group, especially the wish-item-with-contributions edge case noted at the bottom).
3. Build the server skeleton: open a socket, accept incoming connections.
4. Add multi-threading — spin up one thread per connected client so multiple users can be handled at once.
5. Build the dispatch layer: read incoming JSON, extract `"action"`, route to a handler function.
6. Stub every action from `PROTOCOL.md` with a hardcoded response, just to prove round-trip works end to end.
7. Hand this stubbed server to Persons 4, 5, 6 so they can start testing real network calls even before business logic is real.
8. Once Person 3 has business logic methods ready, replace each stub one by one with the real call.
9. Add password hashing at the point where REGISTER/LOGIN touch the password field (coordinate with Person 1 on the Users table column).
10. Handle connection lifecycle correctly: clean disconnects, no leaked threads/sockets when a client closes.
11. Add basic malformed-request handling (bad JSON, unknown action) so the server never crashes on bad input — return a standard ERROR response instead.
12. Support the integration tests (Phase 3 and Phase 7) by running the server and watching logs for thread/connection issues during multi-client testing.

---

## Person 3 — Business Logic (Request Handling)

**Owns:** the logic behind every protocol action, connecting Person 1's queries to Person 2's dispatch layer.

1. Attend kickoff.
2. As soon as Person 1 pushes the Users schema/queries: implement REGISTER and LOGIN logic (calls query layer, returns success/error per protocol).
3. As soon as Friends queries exist: implement ADD_FRIEND, ACCEPT_FRIEND, DECLINE_FRIEND, REMOVE_FRIEND, VIEW_FRIENDS, VIEW_PENDING_REQUESTS.
4. As soon as CatalogItems queries exist: implement VIEW_CATALOG.
5. As soon as WishItems queries exist: implement CREATE_WISH_ITEM, UPDATE_WISH_ITEM, DELETE_WISH_ITEM, VIEW_FRIEND_WISHLIST (remember the "not friends" check on this one).
6. Implement CONTRIBUTE using Person 1's guarded atomic update — check the affected-row result to determine success vs. "amount exceeds remaining price," and detect whether this contribution just completed the item.
7. Implement the notification-trigger logic: the instant CONTRIBUTE completes an item, insert a "buyer" notification for every contributor to that item and a "receiver" notification for the list owner.
8. Implement GET_NOTIFICATIONS and MARK_NOTIFICATION_READ.
9. Hand each finished method to Person 2 to wire into the dispatch layer, one action at a time — don't wait to hand everything over at once.
10. During integration testing: be the first responder for any bug that looks like "wrong data came back" — that's almost always a business logic issue.
11. Specifically verify the contribution race condition under concurrent load (Phase 4, item 37 in the master plan) — this is your feature to defend, work with Person 1 if the guard needs adjusting.

---

## Person 4 — Client: Authentication & Friends

**Owns:** Register/Sign-in, Add/Remove Friend, Accept/Decline Friend Request, View Friends List screens.

1. Attend kickoff.
2. Build the Register/Sign-in screen UI with mock/hardcoded success (no networking yet).
3. Build the Add/Remove Friend screen UI (search box + friend list with remove buttons) with mock data.
4. Build the Accept/Decline Friend Request screen UI (list of incoming requests with two buttons each) with mock data.
5. Build the View Friends List screen UI with mock data.
6. Once Person 2's stubbed server is up: wire Register and Login to real `REGISTER`/`LOGIN` calls per `PROTOCOL.md`.
7. Wire Add Friend to `ADD_FRIEND`, handle the error cases (`"User not found"`, `"Already friends"`, `"Friend request already pending"`) with real UI messages, not silent failures.
8. Wire Accept/Decline screen to `VIEW_PENDING_REQUESTS` (to populate it) and `ACCEPT_FRIEND`/`DECLINE_FRIEND`.
9. Wire Remove Friend to `REMOVE_FRIEND`.
10. Wire View Friends List to `VIEW_FRIENDS`.
11. Add loading/disabled states on buttons during any network call.
12. Participate in both integration tests — specifically hammer the friend-request flow with duplicate/edge-case attempts.

---

## Person 5 — Client: Wish Lists

**Owns:** Create/Update/Delete own wish list, View Friend's Wish List screens.

1. Attend kickoff.
2. Build the own-wish-list screen UI (list view + add/edit/delete controls, pulling from a catalog picker) with mock data.
3. Build the View Friend's Wish List screen UI (read-only list showing progress per item) with mock data.
4. Once Person 2's stubbed server is up: wire the catalog picker to `VIEW_CATALOG`.
5. Wire "add to wish list" to `CREATE_WISH_ITEM`.
6. Wire edit/delete to `UPDATE_WISH_ITEM`/`DELETE_WISH_ITEM`, including surfacing the "can't edit/delete — has contributions" error clearly to the user.
7. Wire View Friend's Wish List to `VIEW_FRIEND_WISHLIST`, showing amount raised vs. price per item (e.g. a progress bar or "200/500 EGP" label) and marking completed items visibly.
8. Add loading/disabled states during network calls.
9. Participate in both integration tests — specifically verify that wish list changes made by one client are visible to a friend viewing it from another client instance.

---

## Person 6 — Client: Contributions, Notifications & GUI Polish

**Owns:** Contribute screen, notification inbox, and the final consistency pass across the whole app.

1. Attend kickoff.
2. Build the Contribute screen UI (pick a friend's item, enter an amount, confirm) with mock data.
3. Build the Notifications inbox UI (list of messages, unread indicator) with mock data.
4. Once Person 2's stubbed server is up: wire the Contribute screen to `CONTRIBUTE`, handling `"Amount exceeds remaining price"` and `"Cannot contribute to your own wish item"` with clear UI feedback.
5. Wire Notifications inbox to `GET_NOTIFICATIONS` and `MARK_NOTIFICATION_READ`.
6. Once Persons 4 and 5 have their screens functionally working: do a full pass across every screen in the app for consistent colors, fonts, icons, and spacing — this is the graded "Friendly GUI" requirement.
7. Add/standardize error message display across all screens (not just your own) so failures never fail silently anywhere in the app.
8. Add loading/disabled states during network calls, both on your own screens and any you find missing it elsewhere.
9. Lead the second integration test (Phase 7): specifically try to break contributions and notifications with 3 simultaneous clients.
10. Own the final demo run-through: confirm the full path (register → add friend → accept → view friend's list → contribute → both notifications fire) works cleanly before recording.

---

## Cross-cutting reminders for everyone

- Nobody writes real networking code against an action not yet in `PROTOCOL.md`.
- Push early, push often — Person 1 can't merge what isn't pushed.
- If you're blocked, say so immediately rather than waiting — with a 2-day timeline there's no slack to absorb silent delays.
- Every screen: disable the action button while waiting on a server response, and show a real error message on failure.
