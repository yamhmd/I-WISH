
        package iwish.ui;

import iwish.model.User;
import iwish.model.WishItem;
import iwish.net.NetworkClient;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FriendWishListApp extends Application {

    private NetworkClient networkClient;

    private VBox wishlistContainer;
    private ComboBox<User> friendComboBox;
    private Button viewButton;

    // Constructor used when opening the screen with the logged-in client.
    public FriendWishListApp(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    // Default constructor for JavaFX.
    public FriendWishListApp() {
    }

    @Override
    public void start(Stage stage) {

        if (networkClient == null ||
                !networkClient.isLoggedIn()) {

            showError(
                    "Not Logged In",
                    "Please login first."
            );

            return;
        }

        Label title =
                new Label(
                        "View Friend's Wish List"
                );

        title.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;"
        );

        friendComboBox =
                new ComboBox<>();

        friendComboBox.setPromptText(
                "Select a friend"
        );

        friendComboBox.setPrefWidth(300);

        viewButton =
                new Button(
                        "View Wish List"
                );

        viewButton.setDisable(true);

        friendComboBox.setOnAction(
                e -> viewButton.setDisable(
                        friendComboBox.getValue() == null
                )
        );

        viewButton.setOnAction(
                e -> loadFriendWishlist()
        );

        HBox friendBox =
                new HBox(
                        10,
                        friendComboBox,
                        viewButton
                );

        friendBox.setAlignment(
                Pos.CENTER_LEFT
        );

        wishlistContainer =
                new VBox(15);

        wishlistContainer.setPadding(
                new Insets(10)
        );

        ScrollPane scrollPane =
                new ScrollPane(
                        wishlistContainer
                );

        scrollPane.setFitToWidth(true);

        VBox topSection =
                new VBox(
                        15,
                        title,
                        friendBox
                );

        topSection.setPadding(
                new Insets(20)
        );

        BorderPane root =
                new BorderPane();

        root.setTop(topSection);
        root.setCenter(scrollPane);

        BorderPane.setMargin(
                scrollPane,
                new Insets(0, 20, 20, 20)
        );

        loadFriends();

        Scene scene =
                new Scene(
                        root,
                        850,
                        650
                );

        stage.setTitle(
                "iWish - Friend's Wish List"
        );

        stage.setScene(scene);
        stage.show();
    }

    // Load accepted friends using VIEW_FRIENDS.
    private void loadFriends() {

        friendComboBox.setDisable(true);
        friendComboBox.setPromptText(
                "Loading friends..."
        );

        try {

            Map<String, Object> response =
                    networkClient.viewFriends();

            if (!NetworkClient.isOk(response)) {

                showError(
                        "Error",
                        NetworkClient.getMessage(
                                response
                        )
                );

                return;
            }

            List<Object> objects =
                    NetworkClient.getList(
                            response,
                            "friends"
                    );

            List<User> friends =
                    new ArrayList<>();

            for (Object object : objects) {

                if (!(object instanceof Map)) {
                    continue;
                }

                Map<?, ?> data =
                        (Map<?, ?>) object;

                Object id =
                        data.get("user_id");

                if (!(id instanceof Number)) {

                    id =
                            data.get("friend_id");
                }

                Object username =
                        data.get("username");

                if (username == null) {

                    username =
                            data.get("friend_username");
                }

                if (!(id instanceof Number)) {
                    continue;
                }

                if (username == null) {

                    username =
                            "User #" +
                                    ((Number) id).intValue();
                }

                // User constructor requires five parameters.
                // Only userId and username are available from the network response.
                User user =
                        new User(
                                ((Number) id).intValue(),
                                username.toString(),
                                null,
                                null,
                                null
                        );

                friends.add(user);
            }

            friendComboBox
                    .getItems()
                    .clear();

            if (friends.isEmpty()) {

                friendComboBox.setPromptText(
                        "No friends available"
                );

                showInformation(
                        "No Friends",
                        "You don't have any accepted friends yet."
                );

                return;
            }

            friendComboBox
                    .getItems()
                    .addAll(friends);

            friendComboBox.setCellFactory(
                    listView ->
                            new ListCell<User>() {

                                @Override
                                protected void updateItem(
                                        User user,
                                        boolean empty) {

                                    super.updateItem(
                                            user,
                                            empty
                                    );

                                    if (empty ||
                                            user == null) {

                                        setText(null);

                                    } else {

                                        setText(
                                                user.getUsername()
                                        );
                                    }
                                }
                            }
            );

            friendComboBox.setButtonCell(
                    new ListCell<User>() {

                        @Override
                        protected void updateItem(
                                User user,
                                boolean empty) {

                            super.updateItem(
                                    user,
                                    empty
                            );

                            if (empty ||
                                    user == null) {

                                setText(null);

                            } else {

                                setText(
                                        user.getUsername()
                                );
                            }
                        }
                    }
            );

            friendComboBox.setPromptText(
                    "Select a friend"
            );

        } catch (Exception e) {

            showError(
                    "Error",
                    "Could not load friends."
            );

            e.printStackTrace();

        } finally {

            friendComboBox.setDisable(false);
        }
    }

    // Load the selected friend's wishlist using VIEW_FRIEND_WISHLIST.
    private void loadFriendWishlist() {

        User selectedFriend =
                friendComboBox.getValue();

        if (selectedFriend == null) {
            return;
        }

        viewButton.setDisable(true);
        viewButton.setText(
                "Loading..."
        );

        friendComboBox.setDisable(true);

        wishlistContainer
                .getChildren()
                .clear();

        try {

            int friendUserId =
                    selectedFriend.getUserId();

            Map<String, Object> response =
                    networkClient.viewFriendWishlist(
                            friendUserId
                    );

            if (!NetworkClient.isOk(response)) {

                showError(
                        "Error",
                        NetworkClient.getMessage(
                                response
                        )
                );

                return;
            }

            List<Object> objects =
                    NetworkClient.getList(
                            response,
                            "wish_items"
                    );

            if (objects.isEmpty()) {

                Label empty =
                        new Label(
                                "This friend has no wish items."
                        );

                empty.setStyle(
                        "-fx-font-size: 18px;"
                );

                wishlistContainer
                        .getChildren()
                        .add(empty);

                return;
            }

            for (Object object : objects) {

                if (!(object instanceof Map)) {
                    continue;
                }

                Map<?, ?> data =
                        (Map<?, ?>) object;

                WishItem item =
                        createWishItem(data);

                if (item != null) {

                    VBox card =
                            createWishItemCard(item);

                    wishlistContainer
                            .getChildren()
                            .add(card);
                }
            }

        } catch (Exception e) {

            showError(
                    "Error",
                    "Could not load friend's wish list."
            );

            e.printStackTrace();

        } finally {

            viewButton.setDisable(false);
            viewButton.setText(
                    "View Wish List"
            );

            friendComboBox.setDisable(false);
        }
    }

    // Convert the network response into a WishItem object.
    private WishItem createWishItem(
            Map<?, ?> data) {

        try {

            Object wishIdObject =
                    data.get("wish_id");

            Object userIdObject =
                    data.get("user_id");

            Object itemIdObject =
                    data.get("item_id");

            Object amountObject =
                    data.get("amount_raised");

            Object completeObject =
                    data.get("is_complete");

            if (!(wishIdObject instanceof Number) ||
                    !(itemIdObject instanceof Number)) {

                return null;
            }

            int wishId =
                    ((Number) wishIdObject).intValue();

            int userId;

            if (userIdObject instanceof Number) {

                userId =
                        ((Number) userIdObject).intValue();

            } else {

                userId =
                        networkClient.getCurrentUserId();
            }

            int itemId =
                    ((Number) itemIdObject).intValue();

            BigDecimal amountRaised =
                    BigDecimal.ZERO;

            if (amountObject != null) {

                amountRaised =
                        new BigDecimal(
                                amountObject.toString()
                        );
            }

            boolean complete =
                    Boolean.parseBoolean(
                            String.valueOf(
                                    completeObject
                            )
                    );

            WishItem item =
                    new WishItem(
                            wishId,
                            userId,
                            itemId,
                            amountRaised,
                            complete
                    );

            Object itemName =
                    data.get("item_name");

            if (itemName != null) {

                item.setItemName(
                        itemName.toString()
                );
            }

            Object itemPrice =
                    data.get("item_price");

            if (itemPrice != null) {

                item.setItemPrice(
                        new BigDecimal(
                                itemPrice.toString()
                        )
                );
            }

            return item;

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }

    // Create one wishlist item card.
    private VBox createWishItemCard(
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

        BigDecimal price =
                item.getItemPrice();

        BigDecimal raised =
                item.getAmountRaised();

        if (price == null) {
            price = BigDecimal.ZERO;
        }

        if (raised == null) {
            raised = BigDecimal.ZERO;
        }

        Label priceLabel =
                new Label(
                        "Price: $" +
                                price
                );

        Label raisedLabel =
                new Label(
                        "Amount Raised: $" +
                                raised
                );

        double progress = 0;

        if (price.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            progress =
                    raised.doubleValue()
                            / price.doubleValue();

            if (progress > 1) {
                progress = 1;
            }

            if (progress < 0) {
                progress = 0;
            }
        }

        ProgressBar progressBar =
                new ProgressBar(
                        progress
                );

        progressBar.setPrefWidth(500);

        Label progressLabel;

        if (item.isComplete()) {

            progressLabel =
                    new Label(
                            "✓ Completed"
                    );

            progressLabel.setStyle(
                    "-fx-font-weight: bold;"
            );

        } else {

            int percentage =
                    (int) (
                            progress * 100
                    );

            progressLabel =
                    new Label(
                            "Progress: " +
                                    percentage +
                                    "%"
                    );
        }

        HBox details =
                new HBox(
                        20,
                        priceLabel,
                        raisedLabel
                );

        details.setAlignment(
                Pos.CENTER_LEFT
        );

        card.getChildren().addAll(
                name,
                details,
                progressBar,
                progressLabel
        );

        return card;
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

