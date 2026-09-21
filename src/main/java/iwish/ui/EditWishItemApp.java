package iwish.ui;

import iwish.db.CatalogItemDAO;
import iwish.db.WishItemDAO;
import iwish.model.CatalogItem;
import iwish.model.WishItem;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class EditWishItemApp extends Application {

    private final WishItemDAO wishItemDAO = new WishItemDAO();
    private final CatalogItemDAO catalogItemDAO = new CatalogItemDAO();

    // مؤقت لحد ما نربطه بالـ Login
    private final int currentUserId = 1;

    @Override
    public void start(Stage stage) {

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label title = new Label("Edit Wish Item");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        ComboBox<WishItem> wishBox = new ComboBox<>();
        wishBox.setPromptText("Select Wish Item");

        ComboBox<CatalogItem> itemBox = new ComboBox<>();
        itemBox.setPromptText("Select New Item");

        Button saveButton = new Button("Save Changes");

        Label message = new Label();

        try {

            // Load user's wishlist
            List<WishItem> wishes =
                    wishItemDAO.getWishItemsByUser(currentUserId);

            wishBox.getItems().addAll(wishes);

            // Load catalog
            List<CatalogItem> items =
                    catalogItemDAO.getAllItems();

            itemBox.getItems().addAll(items);

        } catch (Exception e) {
            message.setText("Error loading data: " + e.getMessage());
        }

        // شكل WishItem داخل ComboBox
        wishBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(WishItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getItemName());
                }
            }
        });

        wishBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(WishItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getItemName());
                }
            }
        });

        // شكل CatalogItem داخل ComboBox
        itemBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(CatalogItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - $" + item.getPrice());
                }
            }
        });

        itemBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CatalogItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - $" + item.getPrice());
                }
            }
        });

        // Save changes
        saveButton.setOnAction(event -> {

            WishItem selectedWish = wishBox.getValue();
            CatalogItem selectedItem = itemBox.getValue();

            if (selectedWish == null || selectedItem == null) {
                message.setText("Please select both items.");
                return;
            }

            try {

                wishItemDAO.updateWishItem(
                        currentUserId,
                        selectedWish.getWishId(),
                        selectedItem.getItemId()
                );

                message.setText("Wish item updated successfully!");

            } catch (IllegalStateException e) {

                message.setText(e.getMessage());

            } catch (Exception e) {

                message.setText("Error: " + e.getMessage());
            }
        });

        root.getChildren().addAll(
                title,
                new Label("Choose Wish Item:"),
                wishBox,
                new Label("Choose New Catalog Item:"),
                itemBox,
                saveButton,
                message
        );

        Scene scene = new Scene(root, 450, 400);

        stage.setTitle("Edit Wish Item");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}