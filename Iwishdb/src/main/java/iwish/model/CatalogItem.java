package iwish.model;

import java.math.BigDecimal;

public class CatalogItem {
    private int itemId;
    private String name;
    private BigDecimal price;
    private String imagePath; // may be null

    public CatalogItem(int itemId, String name, BigDecimal price, String imagePath) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.imagePath = imagePath;
    }

    public int getItemId() { return itemId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getImagePath() { return imagePath; }
}
