package com.iwish.iwishclient.model;

import java.util.List;

/** A friend's wish list, as returned by PROTOCOL.md #14 VIEW_FRIEND_WISHLIST. */
public class FriendWishlist {
    private final String friendUsername;
    private final List<WishItem> wishItems;

    public FriendWishlist(String friendUsername, List<WishItem> wishItems) {
        this.friendUsername = friendUsername;
        this.wishItems = wishItems;
    }

    public String getFriendUsername() { return friendUsername; }
    public List<WishItem> getWishItems() { return wishItems; }
}
