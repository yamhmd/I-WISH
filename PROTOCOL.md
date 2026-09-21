# i-Wish Client-Server Protocol

This is the single source of truth for every message exchanged between Client and Server. No one writes networking code against an action that isn't defined here yet. If you need to change something, edit this file and tell the team in the sync.

## General Rules

- Transport: TCP sockets, one message per request/response, JSON-encoded, newline-terminated.
- Every request has an `"action"` field (string, UPPERCASE_WITH_UNDERSCORES).
- Every response has a `"status"` field: `"OK"` or `"ERROR"`.
- On `"ERROR"`, the response includes a `"message"` field with a human-readable reason. No other fields are guaranteed on error.
- Timestamps are ISO-8601 strings, e.g. `"2026-09-17T14:30:00Z"`.
- Money amounts are numbers (floats), two decimal places of precision expected.
- IDs (`user_id`, `wish_id`, `item_id`, etc.) are integers matching the database primary keys.
- Passwords are never sent back in any response, ever.
- Once a client logs in, the iwish.server should associate `user_id` with that socket/session so later requests don't need to keep re-sending it insecurely — but for simplicity in this project, we still include `user_id` explicitly in every request so the iwish.server can validate it against the session.

## Standard Error Response

```json
{ "status": "ERROR", "message": "Description of what went wrong" }
```

Common `message` values to use consistently: `"Invalid credentials"`, `"Username already taken"`, `"Not friends"`, `"Friend request already pending"`, `"Item not found"`, `"Amount exceeds remaining price"`, `"Unauthorized"`.

---

## 1. REGISTER

**Request**
```json
{ "action": "REGISTER", "username": "ahmed", "email": "ahmed@mail.com", "password": "plaintext_from_client" }
```

**Response (success)**
```json
{ "status": "OK", "user_id": 1, "username": "ahmed" }
```

**Response (error)** — e.g. username/email already exists.

*Note: iwish.server hashes the password before storing it. Client always sends plaintext over this call — hashing is a iwish.server responsibility.*

---

## 2. LOGIN

**Request**
```json
{ "action": "LOGIN", "username": "ahmed", "password": "plaintext_from_client" }
```

**Response (success)**
```json
{ "status": "OK", "user_id": 1, "username": "ahmed" }
```

**Response (error)** — `"Invalid credentials"`.

---

## 3. ADD_FRIEND

**Request**
```json
{ "action": "ADD_FRIEND", "user_id": 1, "friend_username": "mohamed" }
```

**Response (success)**
```json
{ "status": "OK", "message": "Friend request sent" }
```

**Response (error)** — `"User not found"`, `"Friend request already pending"`, `"Already friends"`.

---

## 4. ACCEPT_FRIEND

**Request**
```json
{ "action": "ACCEPT_FRIEND", "user_id": 2, "requester_id": 1 }
```

**Response**
```json
{ "status": "OK", "message": "Friend request accepted" }
```

---

## 5. DECLINE_FRIEND

**Request**
```json
{ "action": "DECLINE_FRIEND", "user_id": 2, "requester_id": 1 }
```

**Response**
```json
{ "status": "OK", "message": "Friend request declined" }
```

---

## 6. REMOVE_FRIEND

**Request**
```json
{ "action": "REMOVE_FRIEND", "user_id": 1, "friend_id": 2 }
```

**Response**
```json
{ "status": "OK", "message": "Friend removed" }
```

---

## 7. VIEW_FRIENDS

**Request**
```json
{ "action": "VIEW_FRIENDS", "user_id": 1 }
```

**Response**
```json
{
  "status": "OK",
  "friends": [
    { "user_id": 2, "username": "mohamed" },
    { "user_id": 3, "username": "sara" }
  ]
}
```

---

## 8. VIEW_PENDING_REQUESTS

*(incoming friend requests awaiting this user's response — needed for the Accept/Decline screen)*

**Request**
```json
{ "action": "VIEW_PENDING_REQUESTS", "user_id": 2 }
```

**Response**
```json
{
  "status": "OK",
  "requests": [
    { "requester_id": 1, "username": "ahmed", "requested_at": "2026-09-17T10:00:00Z" }
  ]
}
```

---

## 9. VIEW_CATALOG

*(the admin-seeded list of items a user can pick from when building their wish list)*

**Request**
```json
{ "action": "VIEW_CATALOG" }
```

**Response**
```json
{
  "status": "OK",
  "items": [
    { "item_id": 10, "name": "Headphones", "price": 500.00 },
    { "item_id": 11, "name": "Watch", "price": 1200.00 }
  ]
}
```

---

## 10. CREATE_WISH_ITEM

**Request**
```json
{ "action": "CREATE_WISH_ITEM", "user_id": 1, "item_id": 10 }
```

**Response (success)**
```json
{ "status": "OK", "wish_id": 55, "item_id": 10, "amount_raised": 0.00, "is_complete": false }
```

**Response (error)** — `"Item already in wish list"`.

---

## 11. UPDATE_WISH_ITEM

*(scope kept minimal — swap which catalog item a wish entry points to, since price/name are catalog-owned, not user-editable)*

**Request**
```json
{ "action": "UPDATE_WISH_ITEM", "user_id": 1, "wish_id": 55, "item_id": 12 }
```

**Response**
```json
{ "status": "OK", "message": "Wish item updated" }
```

**Response (error)** — `"Cannot edit an item with existing contributions"` (recommended rule — see notes below).

---

## 12. DELETE_WISH_ITEM

**Request**
```json
{ "action": "DELETE_WISH_ITEM", "user_id": 1, "wish_id": 55 }
```

**Response**
```json
{ "status": "OK", "message": "Wish item deleted" }
```

**Response (error)** — `"Cannot delete an item with existing contributions"` (recommended rule — see notes below).

---

## 13. VIEW_MY_WISHLIST

*(the logged-in user's own wish list — needed so the client can show wish_id values for update/delete)*

**Request**
```json
{ "action": "VIEW_MY_WISHLIST", "user_id": 1 }
```

**Response (success)**
```json
{
  "status": "OK",
  "wish_items": [
    { "wish_id": 55, "item_id": 10, "name": "Headphones", "price": 500.00, "amount_raised": 0.00, "is_complete": false }
  ]
}
```

---

## 14. VIEW_FRIEND_WISHLIST

**Request**
```json
{ "action": "VIEW_FRIEND_WISHLIST", "user_id": 1, "friend_id": 2 }
```

**Response (success)**
```json
{
  "status": "OK",
  "friend_username": "mohamed",
  "wish_items": [
    { "wish_id": 60, "item_id": 10, "name": "Headphones", "price": 500.00, "amount_raised": 200.00, "is_complete": false }
  ]
}
```

**Response (error)** — `"Not friends"` (only friends can view each other's lists).

---

## 15. CONTRIBUTE

**Request**
```json
{ "action": "CONTRIBUTE", "user_id": 3, "wish_id": 60, "amount": 300.00 }
```

**Response (success)**
```json
{ "status": "OK", "wish_completed": true, "message": "Contribution recorded" }
```

**Response (error)** — `"Amount exceeds remaining price"`, `"Cannot contribute to your own wish item"`, `"Item already complete"`.

*Note: the iwish.server must apply this as one atomic, guarded update (e.g. `UPDATE WishItems SET amount_raised = amount_raised + ? WHERE wish_id = ? AND amount_raised + ? <= price`) so two simultaneous contributions can never push the total past the price. If the guarded update affects zero rows, return the "Amount exceeds remaining price" error.*

---

## 16. GET_NOTIFICATIONS

**Request**
```json
{ "action": "GET_NOTIFICATIONS", "user_id": 1 }
```

**Response**
```json
{
  "status": "OK",
  "notifications": [
    {
      "notif_id": 200,
      "type": "receiver",
      "message": "Your item 'Headphones' was fully funded!",
      "is_read": false,
      "created_at": "2026-09-17T15:00:00Z"
    },
    {
      "notif_id": 201,
      "type": "buyer",
      "message": "Your contribution to 'Headphones' helped complete it!",
      "is_read": false,
      "created_at": "2026-09-17T15:00:00Z"
    }
  ]
}
```

---

## 17. MARK_NOTIFICATION_READ

**Request**
```json
{ "action": "MARK_NOTIFICATION_READ", "user_id": 1, "notif_id": 200 }
```

**Response**
```json
{ "status": "OK", "message": "Notification marked as read" }
```

---

## Notes for the team

- **Editing/deleting a wish item that already has contributions** is a genuine edge case not covered by the original spec. Recommended rule above: block it and return an error, rather than trying to handle partial refunds — simpler and defensible in a demo. Confirm this as a group in the kickoff meeting; if you disagree, update this file.
- Add new actions here the moment you need one the client doesn't yet have — don't improvise a shape in code first and document later.
- Person 2 (Server Networking) owns this file's day-to-day accuracy; anyone can propose an edit but should flag it in the group chat before merging.
