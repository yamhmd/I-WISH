package iwish.ui;

import iwish.model.CatalogItem;
import iwish.model.WishItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MockData {

    public static List<CatalogItem> getCatalogItems() {

        List<CatalogItem> items = new ArrayList<>();

        items.add(new CatalogItem(
                1,
                "Wireless Headphones",
                new BigDecimal("1200.00"),
                null
        ));

        items.add(new CatalogItem(
                2,
                "Smart Watch",
                new BigDecimal("2500.00"),
                null
        ));

        items.add(new CatalogItem(
                3,
                "Gaming Keyboard",
                new BigDecimal("1800.00"),
                null
        ));

        items.add(new CatalogItem(
                4,
                "Bluetooth Speaker",
                new BigDecimal("1500.00"),
                null
        ));

        return items;
    }


    public static List<WishItem> getWishItems() {

        List<WishItem> wishes = new ArrayList<>();

        WishItem wish1 = new WishItem(
                1,
                1,
                1,
                new BigDecimal("500.00"),
                false
        );

        wish1.setItemName("Wireless Headphones");
        wish1.setItemPrice(new BigDecimal("1200.00"));

        wishes.add(wish1);


        WishItem wish2 = new WishItem(
                2,
                1,
                2,
                new BigDecimal("2500.00"),
                true
        );

        wish2.setItemName("Smart Watch");
        wish2.setItemPrice(new BigDecimal("2500.00"));

        wishes.add(wish2);

        return wishes;
    }
}