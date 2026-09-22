package com.iwish.iwishclient.model;

/** One entry from the admin-seeded catalog (PROTOCOL.md #9 VIEW_CATALOG). */
public class CatalogItem {
    private final int itemId;
    private final String name;
    private final double price;

    public CatalogItem(int itemId, String name, double price) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
    }

    public int getItemId() { return itemId; }
    public String getName() { return name; }
    public double getPrice() { return price; }

    @Override
    public String toString() { return name; }
}
