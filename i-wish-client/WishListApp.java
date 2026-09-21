
        package iwish.ui;

import iwish.db.WishItemDAO;
import iwish.net.NetworkClient;
import iwish.model.WishItem;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class WishListApp extends Application {

    private final WishItemDAO wishItemDAO = new WishItemDAO();

    private NetworkClient networkClient;

    private VBox itemsContainer;
    private Button refreshButton;

    // This constructor is used when opening the screen with the logged-in client.
    public WishListApp(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    // Default constructor for JavaFX.
    public WishListApp() {
    }

    @Override
    public void start(Stage stage) {

        if (networkClient == null) {

            showError(
                    "Not Connected",
                    "Network client is not connected."
            );

            return;
        }

        Label title =
                new Label("My Wish List");

        title.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;"
        );

        itemsContainer =
                new VBox(15);

        itemsContainer.setPadding(
                new Insets(10)
        );

        ScrollPane scrollPane =
                new ScrollPane(itemsContainer);

        scrollPane.setFitToWidth(true);

        refreshButton =
                new Button("Refresh");

        refreshButton.setOnAction(
                e -> loadWishList()
        );

        BorderPane root =
                new BorderPane();

        root.setTop(title);
        root.setCenter(scrollPane);
        root.setBottom(refreshButton);

        BorderPane.setMargin(
                title,
                new Insets(20)
        );

        BorderPane.setMargin(
                scrollPane,
                new Insets(0, 20, 10, 20)
        );

        BorderPane.setAlignment(
                refreshButton,
                Pos.CENTER
        );

        BorderPane.setMargin(
                refreshButton,
                new Insets(10)
        );

        loadWishList();

        Scene scene =
                new Scene(root, 800, 600);

        stage.setTitle(
                "iWish - My Wish List"
        );

        stage.setScene(scene);
        stage.show();
    }

    // Load the current user's wishlist.
    private void loadWishList() {

        itemsContainer.getChildren().clear();

        refreshButton.setDisable(true);
        refreshButton.setText("Loading...");

        try {

            List<WishItem> items =
                    wishItemDAO.getWishItemsByUser(
                            networkClient.getCurrentUserId()
                    );

            if (items.isEmpty()) {

                Label empty =
                        new Label(
                                "Your wish list is empty."
                        );

                empty.setStyle(
                        "-fx-font-size: 18px;"
                );

                itemsContainer
                        .getChildren()
                        .add(empty);

                return;
            }

            for (WishItem item : items) {

                VBox card =
                        createWishCard(item);

                itemsContainer
                        .getChildren()
                        .add(card);
            }

        } catch (Exception e) {

            showError(
                    "Error",
                    "Could not load your wish list."
            );

            e.printStackTrace();

        } finally {

            refreshButton.setDisable(false);
            refreshButton.setText("Refresh");
        }
    }

    // Create one wishlist card.
    private VBox createWishCard(
            WishItem item) {

        VBox card =
                new VBox(10);

        card.setPadding(
                new Insets(15)
        );

        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #cccccc;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
        );

        String itemName =
                item.getItemName();

        if (itemName == null ||
                itemName.isBlank()) {

            itemName =
                    "Item #" +
                            item.getItemId();
        }

        Label name =
                new Label(itemName);

        name.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;"
        );

        Label itemId =
                new Label(
                        "Item ID: " +
                                item.getItemId()
                );

        Label wishId =
                new Label(
                        "Wish ID: " +
                                item.getWishId()
                );

        Label amount =
                new Label(
                        "Amount Raised: $" +
                                item.getAmountRaised()
                );

        Label complete =
                new Label(
                        item.isComplete()
                                ? "Completed ✓"
                                : "Not Completed"
                );

        Button editButton =
                new Button("Edit");

        Button deleteButton =
                new Button("Delete");

        editButton.setOnAction(
                e -> editItem(
                        item,
                        editButton
                )
        );

        deleteButton.setOnAction(
                e -> deleteItem(
                        item,
                        deleteButton
                )
        );

        HBox buttons =
                new HBox(
                        10,
                        editButton,
                        deleteButton
                );

        buttons.setAlignment(
                Pos.CENTER_LEFT
        );

        card.getChildren().addAll(
                name,
                wishId,
                itemId,
                amount,
                complete,
                buttons
        );

        return card;
    }

    // Load catalog items and select a new item.
    private void editItem(
            WishItem wishItem,
            Button editButton) {

        editButton.setDisable(true);
        editButton.setText("Loading...");

        try {

            Map<String, Object> response =
                    networkClient.viewCatalog();

            if (!NetworkClient.isOk(response)) {

                showError(
                        "Error",
                        NetworkClient.getMessage(response)
                );

                return;
            }

            List<Object> objects =
                    NetworkClient.getList(
                            response,
                            "items"
                    );

            if (objects.isEmpty()) {

                showError(
                        "Error",
                        "No catalog items available."
                );

                return;
            }

            ComboBox<Object> catalogBox =
                    new ComboBox<>();

            catalogBox.getItems()
                    .addAll(objects);

            catalogBox.setMaxWidth(
                    Double.MAX_VALUE
            );

            catalogBox.setCellFactory(
                    list -> new ListCell<>() {

                        @Override
                        protected void updateItem(
                                Object object,
                                boolean empty) {

                            super.updateItem(
                                    object,
                                    empty
                            );

                            if (empty ||
                                    object == null) {

                                setText(null);

                            } else if (
                                    object instanceof Map
                            ) {

                                Map<?, ?> data =
                                        (Map<?, ?>) object;

                                setText(
                                        String.valueOf(
                                                data.get("name")
                                        )
                                                + " - $"
                                                + String.valueOf(
                                                data.get("price")
                                        )
                                );
                            }
                        }
                    }
            );

            catalogBox.setButtonCell(
                    new ListCell<>() {

                        @Override
                        protected void updateItem(
                                Object object,
                                boolean empty) {

                            super.updateItem(
                                    object,
                                    empty
                            );

                            if (empty ||
                                    object == null) {

                                setText(null);

                            } else if (
                                    object instanceof Map
                            ) {

                                Map<?, ?> data =
                                        (Map<?, ?>) object;

                                setText(
                                        String.valueOf(
                                                data.get("name")
                                        )
                                                + " - $"
                                                + String.valueOf(
                                                data.get("price")
                                        )
                                );
                            }
                        }
                    }
            );

            catalogBox.getSelectionModel()
                    .selectFirst();

            Dialog<ButtonType> dialog =
                    new Dialog<>();

            dialog.setTitle(
                    "Edit Wish Item"
            );

            dialog.setHeaderText(
                    "Choose a new catalog item"
            );

            ButtonType updateButton =
                    new ButtonType(
                            "Update",
                            ButtonBar.ButtonData.OK_DONE
                    );

            dialog.getDialogPane()
                    .getButtonTypes()
                    .addAll(
                            updateButton,
                            ButtonType.CANCEL
                    );

            VBox content =
                    new VBox(
                            10,
                            new Label("Catalog item:"),
                            catalogBox
                    );

            content.setPadding(
                    new Insets(10)
            );

            dialog.getDialogPane()
                    .setContent(content);

            dialog.showAndWait()
                    .ifPresent(result -> {

                        if (result != updateButton) {
                            return;
                        }

                        Object selectedObject =
                                catalogBox.getValue();

                        if (!(selectedObject instanceof Map)) {

                            showError(
                                    "Error",
                                    "Please select a catalog item."
                            );

                            return;
                        }

                        Map<?, ?> data =
                                (Map<?, ?>) selectedObject;

                        Object id =
                                data.get("item_id");

                        if (!(id instanceof Number)) {

                            showError(
                                    "Error",
                                    "Invalid catalog item."
                            );

                            return;
                        }

                        int newItemId =
                                ((Number) id).intValue();

                        updateWishItem(
                                wishItem,
                                newItemId,
                                editButton
                        );
                    });

        } catch (Exception e) {

            showError(
                    "Error",
                    "Could not load catalog."
            );

            e.printStackTrace();

        } finally {

            editButton.setDisable(false);
            editButton.setText("Edit");
        }
    }

    // Send UPDATE_WISH_ITEM through the network.
    private void updateWishItem(
            WishItem wishItem,
            int newItemId,
            Button editButton) {

        editButton.setDisable(true);
        editButton.setText("Updating...");

        try {

            Map<String, Object> response =
                    networkClient.updateWishItem(
                            wishItem.getWishId(),
                            newItemId
                    );

            if (!NetworkClient.isOk(response)) {

                String message =
                        NetworkClient.getMessage(response);

                if (message.isBlank()) {

                    message =
                            "Could not update wish item.";
                }

                throw new IllegalStateException(
                        message
                );
            }

            showInformation(
                    "Success",
                    "Wish item updated successfully."
            );

            loadWishList();

        } catch (IllegalStateException e) {

            showError(
                    "Cannot Edit Wish Item",
                    e.getMessage()
            );

        } catch (Exception e) {

            showError(
                    "Error",
                    "Could not update wish item."
            );

            e.printStackTrace();

        } finally {

            editButton.setDisable(false);
            editButton.setText("Edit");
        }
    }

    // Send DELETE_WISH_ITEM through the network.
    private void deleteItem(
            WishItem wishItem,
            Button deleteButton) {

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Delete Wish Item"
        );

        confirmation.setHeaderText(
                "Delete this wish item?"
        );

        confirmation.setContentText(
                "This action cannot be undone."
        );

        confirmation.showAndWait()
                .ifPresent(result -> {

                    if (result != ButtonType.OK) {
                        return;
                    }

                    deleteButton.setDisable(true);
                    deleteButton.setText(
                            "Deleting..."
                    );

                    try {

                        Map<String, Object> response =
                                networkClient.deleteWishItem(
                                        wishItem.getWishId()
                                );

                        if (!NetworkClient.isOk(response)) {

                            String message =
                                    NetworkClient.getMessage(
                                            response
                                    );

                            if (message.isBlank()) {

                                message =
                                        "Could not delete wish item.";
                            }

                            throw new IllegalStateException(
                                    message
                            );
                        }

                        showInformation(
                                "Success",
                                "Wish item deleted successfully."
                        );

                        loadWishList();

                    } catch (IllegalStateException e) {

                        showError(
                                "Cannot Delete Wish Item",
                                e.getMessage()
                        );

                        deleteButton.setDisable(false);
                        deleteButton.setText("Delete");

                    } catch (Exception e) {

                        showError(
                                "Error",
                                "Could not delete wish item."
                        );

                        e.printStackTrace();

                        deleteButton.setDisable(false);
                        deleteButton.setText("Delete");
                    }
                });
    }

    // Show an information message.
    private void showInformation(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // Show an error message.
    private void showError(
            String title,
            String message) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}

