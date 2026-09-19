package iwish.service;

import iwish.db.CatalogItemDAO;
import iwish.db.FriendDAO;
import iwish.db.UserDAO;
import iwish.db.WishItemDAO;
import iwish.model.CatalogItem;
import iwish.model.WishItem;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WishlistService {

    private final WishItemDAO wishItemDAO = new WishItemDAO();
    private final CatalogItemDAO catalogItemDAO = new CatalogItemDAO();
    private final FriendDAO friendDAO = new FriendDAO();

    // VIEW_CATALOG
    public List<Map<String, Object>> viewCatalog() throws SQLException {
        List<CatalogItem> items = catalogItemDAO.getAllItems();
        return items.stream()
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("item_id", i.getItemId());
                    m.put("name", i.getName());
                    m.put("price", i.getPrice());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // CREATE_WISH_ITEM
    public Map<String, Object> createWishItem(int userId, int itemId) throws SQLException {
        requireCatalogItem(itemId);

        // addWishItem throws IllegalStateException("Item already in wish list")
        WishItem wish = wishItemDAO.addWishItem(userId, itemId);

        Map<String, Object> m = new HashMap<>();
        m.put("wish_id", wish.getWishId());
        m.put("item_id", wish.getItemId());
        m.put("amount_raised", wish.getAmountRaised());
        m.put("is_complete", wish.isComplete());
        return m;
    }

    // UPDATE_WISH_ITEM
    public void updateWishItem(int userId, int wishId, int newItemId) throws SQLException {
        requireCatalogItem(newItemId);

        // updateWishItem throws IllegalStateException("Cannot edit an item with existing contributions")
        boolean updated = wishItemDAO.updateWishItem(userId, wishId, newItemId);
        if (!updated) {
            // either wish_id doesn't exist, or it doesn't belong to this user
            throw new IllegalStateException("Item not found");
        }
    }

    // DELETE_WISH_ITEM
    public void deleteWishItem(int userId, int wishId) throws SQLException {
        // deleteWishItem throws IllegalStateException("Cannot delete an item with existing contributions")
        boolean deleted = wishItemDAO.deleteWishItem(userId, wishId);
        if (!deleted) {
            throw new IllegalStateException("Item not found");
        }
    }

    // VIEW_MY_WISHLIST  (NEW - needed so the GUI can show the user's own list
    // and get the wish_id values required by UPDATE/DELETE_WISH_ITEM)
    public Map<String, Object> viewMyWishlist(int userId) throws SQLException {
        // getFriendWishlist(x) simply returns the wish items owned by user x,
        // so it works for the user's own id too.
        List<WishItem> items = wishItemDAO.getFriendWishlist(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("wish_items", toMaps(items));
        return result;
    }

    // VIEW_FRIEND_WISHLIST
    public Map<String, Object> viewFriendWishlist(int userId, int friendId) throws SQLException {
        // The friendship check must happen here, in the business logic layer,
        // before we touch the friend's wish items at all.
        if (!friendDAO.areFriends(userId, friendId)) {
            throw new IllegalStateException("Not friends");
        }

        List<WishItem> items = wishItemDAO.getFriendWishlist(friendId);

        Map<String, Object> result = new HashMap<>();
        String friendUsername = new UserDAO().findById(friendId).getUsername();
        result.put("friend_username", friendUsername);
        result.put("wish_items", toMaps(items));
        return result;
    }

    // ---- helpers ----

    private void requireCatalogItem(int itemId) throws SQLException {
        boolean exists = catalogItemDAO.getAllItems().stream()
                .anyMatch(i -> i.getItemId() == itemId);
        if (!exists) {
            throw new IllegalStateException("Item not found");
        }
    }

    private List<Map<String, Object>> toMaps(List<WishItem> items) {
        return items.stream()
                .map(w -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("wish_id", w.getWishId());
                    m.put("item_id", w.getItemId());
                    m.put("name", w.getItemName());
                    m.put("price", w.getItemPrice());
                    m.put("amount_raised", w.getAmountRaised());
                    m.put("is_complete", w.isComplete());
                    return m;
                })
                .collect(Collectors.toList());
    }
}