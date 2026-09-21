package iwish.ui;

import com.iwish.iwishclient.session.Session;
import iwish.model.CatalogItem;
import iwish.net.NetworkClient;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AddWishItemApp extends Application {

    // NetworkClient used to communicate with the server
    private NetworkClient networkClient;

    private VBox itemsContainer;

    /*
     * Default constructor.
     * When the screen is opened from the logged-in application,
     * we create the NetworkClient and give it the current user's ID
     * from Person 4's Session.
     */
    public AddWishItemApp() {
    }

    /*
     * Constructor used if another screen already has a NetworkClient.
     */
    public AddWishItemApp(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    @Override
    public void start(Stage stage) {

        // ------------------------------------------------------------
        // Check Login
        // ------------------------------------------------------------

        if (Session.get().getCurrentUser() == null) {

            showError(
                    "Not Logged In",
                    "Please login first."
            );

            return;
        }

        /*
         * If no NetworkClient was passed to this screen,
         * create one for the current logged-in user.
         */
        if (networkClient == null) {

            try {

                networkClient = new NetworkClient();

                /*
                 * Get the real logged-in user ID from Person 4's Session.
                 */
                networkClient.setCurrentUserId(
                        Session.get()
                                .getCurrentUser()
                                .getUserId()
                );

            } catch (Exception e) {

                showError(
                        "Connection Error",
                        "Could not connect to server: "
                                + e.getMessage()
                );

                return;
            }
        }

        // ------------------------------------------------------------
        // UI
        // ------------------------------------------------------------

        Label title =
                new Label("Add Wish Item");

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

        BorderPane root =
                new BorderPane();

        root.setTop(title);
        root.setCenter(scrollPane);

        BorderPane.setMargin(
                title,
                new Insets(20)
        );

        BorderPane.setMargin(
                scrollPane,
                new Insets(0, 20, 20, 20)
        );

        // Load catalog from server
        loadCatalog();

        Scene scene =
                new Scene(root, 800, 600);

        stage.setTitle(
                "iWish - Add Wish Item"
        );

        stage.setScene(scene);
        stage.show();
    }

    // ------------------------------------------------------------
    // VIEW_CATALOG
    // ------------------------------------------------------------

    private void loadCatalog() {

        itemsContainer
                .getChildren()
                .clear();

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

            List<Object> items =
                    NetworkClient.getList(
                            response,
                            "items"
                    );

            if (items.isEmpty()) {

                Label empty =
                        new Label(
                                "No catalog items available."
                        );

                empty.setStyle(
                        "-fx-font-size: 18px;"
                );

                itemsContainer
                        .getChildren()
                        .add(empty);

                return;
            }

            for (Object object : items) {

                if (!(object instanceof Map)) {
                    continue;
                }

                Map<?, ?> data =
                        (Map<?, ?>) object;

                int itemId =
                        ((Number) data.get("item_id"))
                                .intValue();

                String name =
                        String.valueOf(
                                data.get("name")
                        );

                BigDecimal price =
                        new BigDecimal(
                                String.valueOf(
                                        data.get("price")
                                )
                        );

                CatalogItem item =
                        new CatalogItem(
                                itemId,
                                name,
                                price,
                                null
                        );

                itemsContainer
                        .getChildren()
                        .add(
                                createItemCard(item)
                        );
            }

        } catch (Exception e) {

            showError(
                    "Error",
                    "Could not load catalog: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // CREATE_WISH_ITEM
    // ------------------------------------------------------------

    private VBox createItemCard(
            CatalogItem item) {

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

        Label name =
                new Label(
                        item.getName()
                );

        name.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;"
        );

        Label price =
                new Label(
                        "Price: $" +
                                item.getPrice()
                );

        Button addButton =
                new Button(
                        "Add to Wish List"
                );

        addButton.setOnAction(
                e -> addItem(
                        item,
                        addButton
                )
        );

        HBox bottom =
                new HBox(
                        10,
                        price,
                        addButton
                );

        bottom.setAlignment(
                Pos.CENTER_LEFT
        );

        card.getChildren().addAll(
                name,
                bottom
        );

        return card;
    }

    private void addItem(
            CatalogItem item,
            Button addButton) {

        addButton.setDisable(true);
        addButton.setText("Adding...");

        try {

            Map<String, Object> response =
                    networkClient.createWishItem(
                            item.getItemId()
                    );

            if (!NetworkClient.isOk(response)) {

                String message =
                        NetworkClient.getMessage(
                                response
                        );

                if (message.isBlank()) {
                    message = "Could not add item.";
                }

                throw new IllegalStateException(
                        message
                );
            }

            addButton.setText("Added ✓");

            showInformation(
                    "Success",
                    item.getName() +
                            " was added to your wish list."
            );

        } catch (IllegalStateException e) {

            addButton.setDisable(false);
            addButton.setText(
                    "Add to Wish List"
            );

            showError(
                    "Cannot Add Item",
                    e.getMessage()
            );

        } catch (Exception e) {

            addButton.setDisable(false);
            addButton.setText(
                    "Add to Wish List"
            );

            showError(
                    "Error",
                    e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // ALERTS
    // ------------------------------------------------------------

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

    // ------------------------------------------------------------
    // MAIN
    // ------------------------------------------------------------

    public static void main(String[] args) {

        launch(args);
    }
}