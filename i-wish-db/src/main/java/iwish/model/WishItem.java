package iwish.model;

import java.math.BigDecimal;

public class WishItem {
    private int wishId;
    private int userId;
    private int itemId;
    private BigDecimal amountRaised;
    private boolean complete;

    // Populated on queries that join CatalogItems for display
    // (name/price of the underlying catalog item). Null/ignored otherwise.
    private String itemName;
    private BigDecimal itemPrice;

    public WishItem(int wishId, int userId, int itemId, BigDecimal amountRaised, boolean complete) {
        this.wishId = wishId;
        this.userId = userId;
        this.itemId = itemId;
        this.amountRaised = amountRaised;
        this.complete = complete;
    }

    public int getWishId() { return wishId; }
    public int getUserId() { return userId; }
    public int getItemId() { return itemId; }
    public BigDecimal getAmountRaised() { return amountRaised; }
    public boolean isComplete() { return complete; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public BigDecimal getItemPrice() { return itemPrice; }
    public void setItemPrice(BigDecimal itemPrice) { this.itemPrice = itemPrice; }
}
