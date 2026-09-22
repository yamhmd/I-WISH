package com.iwish.iwishclient.model;

/**
 * One item on somebody's wish list (own or a friend's). Shape matches
 * PROTOCOL.md's "wish_items" entries (#10-14).
 */
public class WishItem {
    private final int wishId;
    private final int itemId;
    private final String name;
    private final double price;
    private final double amountRaised;
    private final boolean complete;

    public WishItem(int wishId, int itemId, String name, double price,
                     double amountRaised, boolean complete) {
        this.wishId = wishId;
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.amountRaised = amountRaised;
        this.complete = complete;
    }

    public int getWishId() { return wishId; }
    public int getItemId() { return itemId; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public double getAmountRaised() { return amountRaised; }
    public boolean isComplete() { return complete; }

    /** Editing/deleting an item that already has money on it is blocked (see PROTOCOL.md notes). */
    public boolean hasContributions() { return amountRaised > 0; }

    public double getRemaining() { return Math.max(0, price - amountRaised); }

    @Override
    public String toString() { return name; }
}
