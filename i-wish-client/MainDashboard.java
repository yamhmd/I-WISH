
        package iwish.ui;

import com.iwish.iwishclient.session.Session;
import com.iwish.iwishclient.ui.AuthScreen;
import com.iwish.iwishclient.ui.MainScreen;

import iwish.net.Protocol;
import iwish.model.CatalogItem;
import iwish.model.User;
import iwish.model.WishItem;
import iwish.net.NetworkClient;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class MainDashboard extends Application {

    private BorderPane mainLayout;
    private StackPane contentArea;

    private NetworkClient networkClient;

    private int currentUserId;

    private Stage mainStage;

    @Override
    public void start(Stage stage) {

        this.mainStage = stage;

        if (Session.get().getCurrentUser() == null) {
            showLoginScreen(stage);
            return;
        }

        initializeDashboard(stage);
    }

    // ================================================================
    // LOGIN
    // ================================================================

    private void showLoginScreen(Stage stage) {

        AuthScreen authScreen = new AuthScreen(() -> {
            initializeDashboard(stage);
        });

        Scene scene = new Scene(authScreen, 420, 520);

        stage.setTitle("i-Wish");
        stage.setScene(scene);
        stage.show();
    }

    // ================================================================
    // INITIALIZE DASHBOARD
    // ================================================================

    private void initializeDashboard(Stage stage) {

        if (Session.get().getCurrentUser() == null) {
            showLoginScreen(stage);
            return;
        }

        currentUserId =
                Session.get()
                        .getCurrentUser()
                        .getUserId();

        // ============================================================
        // NETWORK CLIENT
        // ============================================================

        try {

            if (networkClient != null) {
                networkClient.close();
            }

            networkClient = new NetworkClient();

            networkClient.setCurrentUserId(currentUserId);

        } catch (Exception e) {

            showConnectionError(
                    stage,
                    e.getMessage()
            );

            return;
        }

        // ============================================================
        // MAIN LAYOUT
        // ============================================================

        mainLayout = new BorderPane();

        // ============================================================
        // SIDEBAR
        // ============================================================

        VBox sidebar = new VBox(15);

        sidebar.setPadding(new Insets(25));
        sidebar.setPrefWidth(230);

        // ============================================================
        // LOGO
        // ============================================================

        Label logo = new Label("iWish");

        logo.setStyle(
                "-fx-font-size: 30px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #6a2c91;"
        );

        // ============================================================
        // USER
        // ============================================================

        Label userLabel =
                new Label(
                        "Hi, " +
                                Session.get()
                                        .getCurrentUser()
                                        .getUsername()
                );

        userLabel.setStyle(
                "-fx-font-size: 14px;"
        );

        // ============================================================
        // PERSON 5 - WISH LIST
        // ============================================================

        Label wishlistSection =
                new Label("WISH LIST");

        wishlistSection.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #777777;"
        );

        Button myWishlistButton =
                new Button("My Wish List");

        Button addWishButton =
                new Button("Add Wish Item");

        Button editWishButton =
                new Button("Edit Wish Item");

        Button deleteWishButton =
                new Button("Delete Wish Item");

        Button friendWishButton =
                new Button("Friend Wish List");

        // ============================================================
        // PERSON 4
        // ============================================================

        Label friendsSection =
                new Label("FRIENDS");

        friendsSection.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #777777;"
        );

        /*
         * بدل 3 أزرار منفصلين:
         *
         * Friends
         * Add / Remove Friend
         * Requests
         *
         * هنفتح MainScreen بتاعة Person 4
         * والـ MainScreen فيها الـ 3 Tabs.
         */

        Button friendsManagementButton =
                new Button("Friends Management");

        // ============================================================
        // LOGOUT
        // ============================================================

        Button logoutButton =
                new Button("Logout");

        // ============================================================
        // BUTTON STYLE
        // ============================================================

        Button[] buttons = {

                myWishlistButton,
                addWishButton,
                editWishButton,
                deleteWishButton,
                friendWishButton,

                friendsManagementButton,

                logoutButton
        };

        for (Button button : buttons) {

            button.setMaxWidth(
                    Double.MAX_VALUE
            );

            button.setPrefHeight(40);

            button.setStyle(
                    "-fx-background-color: #f0e5f7;" +
                            "-fx-text-fill: #4b1f68;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 8;"
            );
        }

        logoutButton.setStyle(
                "-fx-background-color: #d9534f;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;"
        );

        // ============================================================
        // SIDEBAR CONTENT
        // ============================================================

        Region spacer =
                new Region();

        VBox.setVgrow(
                spacer,
                Priority.ALWAYS
        );

        sidebar.getChildren().addAll(

                logo,
                userLabel,

                wishlistSection,

                myWishlistButton,
                addWishButton,
                editWishButton,
                deleteWishButton,
                friendWishButton,

                friendsSection,

                friendsManagementButton,

                spacer,

                logoutButton
        );

        // ============================================================
        // CONTENT AREA
        // ============================================================

        contentArea =
                new StackPane();

        contentArea.setPadding(
                new Insets(30)
        );

        mainLayout.setLeft(sidebar);

        mainLayout.setCenter(
                contentArea
        );

        // ============================================================
        // PERSON 5 ACTIONS
        // ============================================================

        myWishlistButton.setOnAction(
                event -> showMyWishlist()
        );

        addWishButton.setOnAction(
                event -> showAddWish()
        );

        editWishButton.setOnAction(
                event -> showEditWish()
        );

        deleteWishButton.setOnAction(
                event -> showDeleteWish()
        );

        friendWishButton.setOnAction(
                event -> showFriendWishList()
        );

        // ============================================================
        // PERSON 4 ACTION
        // ============================================================

        friendsManagementButton.setOnAction(
                event -> showFriendsManagement()
        );

        // ============================================================
        // LOGOUT
        // ============================================================

        logoutButton.setOnAction(event -> {

            try {

                if (networkClient != null) {

                    networkClient.close();

                    networkClient = null;
                }

            } catch (Exception ignored) {
            }

            Session.get().logout();

            showLoginScreen(stage);
        });

        // ============================================================
        // SHOW DASHBOARD
        // ============================================================

        Scene scene =
                new Scene(
                        mainLayout,
                        1100,
                        700
                );

        stage.setTitle("iWish");

        stage.setScene(scene);

        stage.show();

        showMyWishlist();
    }

    // ================================================================
    // PERSON 4 - FRIENDS MANAGEMENT
    // ================================================================

    private void showFriendsManagement() {

        try {

            /*
             * MainScreen هي شاشة Person 4 الأساسية.
             *
             * جواها:
             *
             * Tab 1 -> Friends
             * Tab 2 -> Add / Remove Friend
             * Tab 3 -> Requests
             */

            MainScreen friendsScreen =
                    new MainScreen();

            friendsScreen.setMaxSize(
                    Double.MAX_VALUE,
                    Double.MAX_VALUE
            );

            contentArea
                    .getChildren()
                    .setAll(friendsScreen);

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Could not open Friends Management:\n" +
                            e.getMessage()
            );
        }
    }

    // ================================================================
    // CONNECTION ERROR
    // ================================================================

    private void showConnectionError(
            Stage stage,
            String error
    ) {

        VBox page =
                new VBox(20);

        page.setPadding(
                new Insets(20)
        );

        Label title =
                new Label("iWish");

        title.setStyle(
                "-fx-font-size: 30px;" +
                        "-fx-font-weight: bold;"
        );

        Label message =
                new Label(
                        "Could not connect to server.\n\n" +
                                "Error: " +
                                error
                );

        message.setStyle(
                "-fx-font-size: 16px;"
        );

        Button retryButton =
                new Button("Retry");

        retryButton.setOnAction(
                event -> initializeDashboard(stage)
        );

        page.getChildren().addAll(
                title,
                message,
                retryButton
        );

        Scene scene =
                new Scene(
                        page,
                        600,
                        400
                );

        stage.setScene(scene);

        stage.show();
    }

    // ================================================================
    // MY WISHLIST
    // ================================================================

    private void showMyWishlist() {

        VBox page =
                new VBox(20);

        page.setPadding(
                new Insets(10)
        );

        Label title =
                new Label("My Wish List");

        title.setStyle(
                "-fx-font-size: 30px;" +
                        "-fx-font-weight: bold;"
        );

        Label message =
                new Label();

        FlowPane cards =
                new FlowPane();

        cards.setHgap(20);
        cards.setVgap(20);

        ProgressIndicator loading =
                new ProgressIndicator();

        loading.setPrefSize(40, 40);

        page.getChildren().addAll(
                title,
                message,
                loading,
                cards
        );

        contentArea
                .getChildren()
                .setAll(page);

        loading.setVisible(false);

        message.setText(
                "Your wishlist will be loaded here after the " +
                        "server protocol provides a VIEW_MY_WISHLIST action."
        );
    }

    // ================================================================
    // ADD WISH
    // ================================================================

    private void showAddWish() {

        VBox page =
                new VBox(20);

        page.setPadding(
                new Insets(10)
        );

        Label title =
                new Label("Add Wish Item");

        title.setStyle(
                "-fx-font-size: 30px;" +
                        "-fx-font-weight: bold;"
        );

        ComboBox<CatalogItem> itemBox =
                new ComboBox<>();

        itemBox.setPromptText(
                "Select an item"
        );

        Button addButton =
                new Button(
                        "Add to Wish List"
                );

        Label message =
                new Label();

        ProgressIndicator loading =
                new ProgressIndicator();

        loading.setPrefSize(
                30,
                30
        );

        loading.setVisible(false);

        page.getChildren().addAll(

                title,

                new Label(
                        "Choose an item:"
                ),

                itemBox,

                addButton,

                loading,

                message
        );

        contentArea
                .getChildren()
                .setAll(page);

        loadCatalog(
                itemBox,
                addButton,
                message,
                loading
        );

        addButton.setOnAction(event -> {

            CatalogItem selectedItem =
                    itemBox.getValue();

            if (selectedItem == null) {

                message.setText(
                        "Please select an item."
                );

                return;
            }

            setLoading(
                    true,
                    addButton,
                    itemBox,
                    loading,
                    message,
                    "Adding item..."
            );

            Task<Map<String, Object>> task =
                    new Task<>() {

                        @Override
                        protected Map<String, Object> call()
                                throws Exception {

                            return networkClient.createWishItem(
                                    selectedItem.getItemId()
                            );
                        }
                    };

            task.setOnSucceeded(event2 -> {

                Map<String, Object> response =
                        task.getValue();

                setLoading(
                        false,
                        addButton,
                        itemBox,
                        loading,
                        message,
                        ""
                );

                if (NetworkClient.isOk(response)) {

                    message.setText(
                            "Item added successfully!"
                    );

                    itemBox.setValue(null);

                } else {

                    message.setText(
                            "Error: " +
                                    NetworkClient.getMessage(
                                            response
                                    )
                    );
                }
            });

            task.setOnFailed(event2 -> {

                setLoading(
                        false,
                        addButton,
                        itemBox,
                        loading,
                        message,
                        ""
                );

                message.setText(
                        "Network error: " +
                                task.getException()
                                        .getMessage()
                );
            });

            Thread thread =
                    new Thread(task);

            thread.setDaemon(true);

            thread.start();
        });
    }

    // ================================================================
    // LOAD CATALOG
    // ================================================================

    private void loadCatalog(
            ComboBox<CatalogItem> itemBox,
            Button button,
            Label message,
            ProgressIndicator loading
    ) {

        itemBox.setDisable(true);

        button.setDisable(true);

        loading.setVisible(true);

        message.setText(
                "Loading catalog..."
        );

        Task<Map<String, Object>> task =
                new Task<>() {

                    @Override
                    protected Map<String, Object> call()
                            throws Exception {

                        return networkClient.viewCatalog();
                    }
                };

        task.setOnSucceeded(event -> {

            loading.setVisible(false);

            Map<String, Object> response =
                    task.getValue();

            if (!NetworkClient.isOk(response)) {

                message.setText(
                        "Error: " +
                                NetworkClient.getMessage(
                                        response
                                )
                );

                itemBox.setDisable(false);
                button.setDisable(false);

                return;
            }

            List<Object> items =
                    NetworkClient.getList(
                            response,
                            "items"
                    );

            itemBox.getItems().clear();

            for (Object object : items) {

                if (!(object instanceof Map<?, ?> map)) {
                    continue;
                }

                try {

                    int id =
                            numberValue(
                                    map.get("item_id")
                            );

                    String name =
                            String.valueOf(
                                    map.get("name")
                            );

                    BigDecimal price =
                            decimalValue(
                                    map.get("price")
                            );

                    CatalogItem item =
                            new CatalogItem(
                                    id,
                                    name,
                                    price,
                                    null
                            );

                    itemBox
                            .getItems()
                            .add(item);

                } catch (Exception e) {

                    System.out.println(
                            "Could not parse catalog item: " +
                                    e.getMessage()
                    );
                }
            }

            itemBox.setDisable(false);

            button.setDisable(false);

            message.setText(
                    "Catalog loaded successfully."
            );
        });

        task.setOnFailed(event -> {

            loading.setVisible(false);

            itemBox.setDisable(false);

            button.setDisable(false);

            message.setText(
                    "Network error: " +
                            task.getException()
                                    .getMessage()
            );
        });

        Thread thread =
                new Thread(task);

        thread.setDaemon(true);

        thread.start();

        setCatalogCellFactory(itemBox);
    }

    // ================================================================
    // CATALOG CELL FACTORY
    // ================================================================

    private void setCatalogCellFactory(
            ComboBox<CatalogItem> itemBox
    ) {

        itemBox.setCellFactory(
                list -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            CatalogItem item,
                            boolean empty
                    ) {

                        super.updateItem(
                                item,
                                empty
                        );

                        if (empty || item == null) {

                            setText(null);

                        } else {

                            setText(
                                    item.getName() +
                                            " - $" +
                                            item.getPrice()
                            );
                        }
                    }
                }
        );

        itemBox.setButtonCell(
                new ListCell<>() {

                    @Override
                    protected void updateItem(
                            CatalogItem item,
                            boolean empty
                    ) {

                        super.updateItem(
                                item,
                                empty
                        );

                        if (empty || item == null) {

                            setText(null);

                        } else {

                            setText(
                                    item.getName() +
                                            " - $" +
                                            item.getPrice()
                            );
                        }
                    }
                }
        );
    }

    // ================================================================
    // EDIT WISH
    // ================================================================

    private void showEditWish() {

        VBox page =
                new VBox(20);

        page.setPadding(
                new Insets(10)
        );

        Label title =
                new Label("Edit Wish Item");

        title.setStyle(
                "-fx-font-size: 30px;" +
                        "-fx-font-weight: bold;"
        );

        Label info =
                new Label(
                        "Select the wish item you want to edit."
                );

        ComboBox<WishItem> wishBox =
                new ComboBox<>();

        wishBox.setPromptText(
                "Select Wish Item"
        );

        ComboBox<CatalogItem> itemBox =
                new ComboBox<>();

        itemBox.setPromptText(
                "Select New Item"
        );

        Button saveButton =
                new Button(
                        "Save Changes"
                );

        Label message =
                new Label();

        ProgressIndicator loading =
                new ProgressIndicator();

        loading.setPrefSize(
                30,
                30
        );

        loading.setVisible(false);

        page.getChildren().addAll(

                title,
                info,
                wishBox,

                new Label(
                        "Choose New Item:"
                ),

                itemBox,

                saveButton,
                loading,
                message
        );

        contentArea
                .getChildren()
                .setAll(page);

        loadCatalog(
                itemBox,
                saveButton,
                message,
                loading
        );

        message.setText(
                "Wishlist loading requires VIEW_MY_WISHLIST " +
                        "in the protocol."
        );

        saveButton.setOnAction(event -> {

            WishItem selectedWish =
                    wishBox.getValue();

            CatalogItem selectedItem =
                    itemBox.getValue();

            if (selectedWish == null) {

                message.setText(
                        "Please select a Wish Item."
                );

                return;
            }

            if (selectedItem == null) {

                message.setText(
                        "Please select a New Item."
                );

                return;
            }

            setLoading(
                    true,
                    saveButton,
                    wishBox,
                    itemBox,
                    loading,
                    message,
                    "Saving changes..."
            );

            Task<Map<String, Object>> task =
                    new Task<>() {

                        @Override
                        protected Map<String, Object> call()
                                throws Exception {

                            return networkClient.updateWishItem(
                                    selectedWish.getWishId(),
                                    selectedItem.getItemId()
                            );
                        }
                    };

            task.setOnSucceeded(event2 -> {

                setLoading(
                        false,
                        saveButton,
                        wishBox,
                        itemBox,
                        loading,
                        message,
                        ""
                );

                Map<String, Object> response =
                        task.getValue();

                if (NetworkClient.isOk(response)) {

                    message.setText(
                            "Wish item updated successfully!"
                    );

                    wishBox.setValue(null);

                    itemBox.setValue(null);

                } else {

                    message.setText(
                            "Error: " +
                                    NetworkClient.getMessage(
                                            response
                                    )
                    );
                }
            });

            task.setOnFailed(event2 -> {

                setLoading(
                        false,
                        saveButton,
                        wishBox,
                        itemBox,
                        loading,
                        message,
                        ""
                );

                message.setText(
                        "Network error: " +
                                task.getException()
                                        .getMessage()
                );
            });

            Thread thread =
                    new Thread(task);

            thread.setDaemon(true);

            thread.start();
        });
    }

    // ================================================================
    // DELETE WISH
    // ================================================================

    private void showDeleteWish() {

        VBox page =
                new VBox(20);

        page.setPadding(
                new Insets(10)
        );

        Label title =
                new Label(
                        "Delete Wish Item"
                );

        title.setStyle(
                "-fx-font-size: 30px;" +
                        "-fx-font-weight: bold;"
        );

        ComboBox<WishItem> wishBox =
                new ComboBox<>();

        wishBox.setPromptText(
                "Select Wish Item"
        );

        Button deleteButton =
                new Button("Delete");

        Label message =
                new Label();

        ProgressIndicator loading =
                new ProgressIndicator();

        loading.setPrefSize(
                30,
                30
        );

        loading.setVisible(false);

        page.getChildren().addAll(

                title,
                wishBox,
                deleteButton,
                loading,
                message
        );

        contentArea
                .getChildren()
                .setAll(page);

        message.setText(
                "Wishlist loading requires VIEW_MY_WISHLIST " +
                        "in the protocol."
        );

        deleteButton.setOnAction(event -> {

            WishItem selectedWish =
                    wishBox.getValue();

            if (selectedWish == null) {

                message.setText(
                        "Please select a wish item."
                );

                return;
            }

            Alert confirmation =
                    new Alert(
                            Alert.AlertType.CONFIRMATION
                    );

            confirmation.setTitle(
                    "Confirm Delete"
            );

            confirmation.setHeaderText(
                    "Delete " +
                            selectedWish.getItemName() +
                            "?"
            );

            confirmation.setContentText(
                    "Are you sure you want to delete this item?"
            );

            ButtonType result =
                    confirmation
                            .showAndWait()
                            .orElse(
                                    ButtonType.CANCEL
                            );

            if (result != ButtonType.OK) {
                return;
            }

            setLoading(
                    true,
                    deleteButton,
                    wishBox,
                    loading,
                    message,
                    "Deleting item..."
            );

            Task<Map<String, Object>> task =
                    new Task<>() {

                        @Override
                        protected Map<String, Object> call()
                                throws Exception {

                            return networkClient.deleteWishItem(
                                    selectedWish.getWishId()
                            );
                        }
                    };

            task.setOnSucceeded(event2 -> {

                setLoading(
                        false,
                        deleteButton,
                        wishBox,
                        loading,
                        message,
                        ""
                );

                Map<String, Object> response =
                        task.getValue();

                if (NetworkClient.isOk(response)) {

                    wishBox
                            .getItems()
                            .remove(selectedWish);

                    wishBox.setValue(null);

                    message.setText(
                            "Wish item deleted successfully!"
                    );

                } else {

                    String error =
                            NetworkClient.getMessage(
                                    response
                            );

                    if (error.equals(
                            Protocol.Errors.CANNOT_DELETE
                    )) {

                        message.setText(
                                "Cannot delete this item because " +
                                        "it already has contributions."
                        );

                    } else {

                        message.setText(
                                "Error: " +
                                        error
                        );
                    }
                }
            });

            task.setOnFailed(event2 -> {

                setLoading(
                        false,
                        deleteButton,
                        wishBox,
                        loading,
                        message,
                        ""
                );

                message.setText(
                        "Network error: " +
                                task.getException()
                                        .getMessage()
                );
            });

            Thread thread =
                    new Thread(task);

            thread.setDaemon(true);

            thread.start();
        });

        wishBox.setCellFactory(
                list -> createWishListCell()
        );

        wishBox.setButtonCell(
                createWishListCell()
        );
    }

    // ================================================================
    // FRIEND WISHLIST
    // ================================================================

    private void showFriendWishList() {

        VBox page =
                new VBox(20);

        page.setPadding(
                new Insets(20)
        );

        Label title =
                new Label(
                        "Friend Wish List"
                );

        title.setStyle(
                "-fx-font-size: 30px;" +
                        "-fx-font-weight: bold;"
        );

        ComboBox<User> friendBox =
                new ComboBox<>();

        friendBox.setPromptText(
                "Select a friend"
        );

        Label message =
                new Label();

        ProgressIndicator loading =
                new ProgressIndicator();

        loading.setPrefSize(
                30,
                30
        );

        loading.setVisible(false);

        FlowPane wishContainer =
                new FlowPane();

        wishContainer.setHgap(15);
        wishContainer.setVgap(15);

        page.getChildren().addAll(

                title,
                friendBox,
                loading,
                message,
                wishContainer
        );

        contentArea
                .getChildren()
                .setAll(page);

        loadFriends(
                friendBox,
                message,
                loading
        );

        friendBox.setCellFactory(
                list -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            User friend,
                            boolean empty
                    ) {

                        super.updateItem(
                                friend,
                                empty
                        );

                        if (empty || friend == null) {

                            setText(null);

                        } else {

                            setText(
                                    friend.getUsername()
                            );
                        }
                    }
                }
        );

        friendBox.setButtonCell(
                new ListCell<>() {

                    @Override
                    protected void updateItem(
                            User friend,
                            boolean empty
                    ) {

                        super.updateItem(
                                friend,
                                empty
                        );

                        if (empty || friend == null) {

                            setText(null);

                        } else {

                            setText(
                                    friend.getUsername()
                            );
                        }
                    }
                }
        );

        friendBox.setOnAction(event -> {

            User selectedFriend =
                    friendBox.getValue();

            wishContainer
                    .getChildren()
                    .clear();

            message.setText("");

            if (selectedFriend == null) {
                return;
            }

            friendBox.setDisable(true);

            loading.setVisible(true);

            message.setText(
                    "Loading friend's wish list..."
            );

            Task<Map<String, Object>> task =
                    new Task<>() {

                        @Override
                        protected Map<String, Object> call()
                                throws Exception {

                            return networkClient
                                    .viewFriendWishlist(
                                            selectedFriend.getUserId()
                                    );
                        }
                    };

            task.setOnSucceeded(event2 -> {

                friendBox.setDisable(false);

                loading.setVisible(false);

                Map<String, Object> response =
                        task.getValue();

                if (!NetworkClient.isOk(response)) {

                    String error =
                            NetworkClient.getMessage(
                                    response
                            );

                    if (error.equals(
                            Protocol.Errors.NOT_FRIENDS
                    )) {

                        message.setText(
                                "You are not friends with this user."
                        );

                    } else {

                        message.setText(
                                "Error: " +
                                        error
                        );
                    }

                    return;
                }

                List<Object> wishes =
                        NetworkClient.getList(
                                response,
                                "wish_items"
                        );

                if (wishes.isEmpty()) {

                    message.setText(
                            "This friend has no wish items."
                    );

                    return;
                }

                for (Object object : wishes) {

                    if (!(object instanceof Map<?, ?> map)) {
                        continue;
                    }

                    try {

                        WishItem wish =
                                createWishFromResponse(
                                        map
                                );

                        wishContainer
                                .getChildren()
                                .add(
                                        createWishCard(
                                                wish
                                        )
                                );

                    } catch (Exception e) {

                        System.out.println(
                                "Could not parse friend wish: " +
                                        e.getMessage()
                        );
                    }
                }

                message.setText(
                        "Friend's wish list loaded successfully!"
                );
            });

            task.setOnFailed(event2 -> {

                friendBox.setDisable(false);

                loading.setVisible(false);

                message.setText(
                        "Network error: " +
                                task.getException()
                                        .getMessage()
                );
            });

            Thread thread =
                    new Thread(task);

            thread.setDaemon(true);

            thread.start();
        });
    }

    // ================================================================
    // LOAD FRIENDS
    // ================================================================

    private void loadFriends(
            ComboBox<User> friendBox,
            Label message,
            ProgressIndicator loading
    ) {

        friendBox.setDisable(true);

        loading.setVisible(true);

        message.setText(
                "Loading friends..."
        );

        Task<Map<String, Object>> task =
                new Task<>() {

                    @Override
                    protected Map<String, Object> call()
                            throws Exception {

                        return networkClient.viewFriends();
                    }
                };

        task.setOnSucceeded(event -> {

            friendBox.setDisable(false);

            loading.setVisible(false);

            Map<String, Object> response =
                    task.getValue();

            if (!NetworkClient.isOk(response)) {

                message.setText(
                        "Error: " +
                                NetworkClient.getMessage(
                                        response
                                )
                );

                return;
            }

            List<Object> friends =
                    NetworkClient.getList(
                            response,
                            "friends"
                    );

            friendBox
                    .getItems()
                    .clear();

            for (Object object : friends) {

                if (!(object instanceof Map<?, ?> map)) {
                    continue;
                }

                try {

                    int id =
                            numberValue(
                                    map.get("user_id")
                            );

                    String username =
                            String.valueOf(
                                    map.get("username")
                            );

                    User user =
                            new User(
                                    id,
                                    username,
                                    "",
                                    "",
                                    new Timestamp(
                                            System.currentTimeMillis()
                                    )
                            );

                    friendBox
                            .getItems()
                            .add(user);

                } catch (Exception e) {

                    System.out.println(
                            "Could not parse friend: " +
                                    e.getMessage()
                    );
                }
            }

            message.setText(
                    "Friends loaded successfully."
            );
        });

        task.setOnFailed(event -> {

            friendBox.setDisable(false);

            loading.setVisible(false);

            message.setText(
                    "Network error: " +
                            task.getException()
                                    .getMessage()
            );
        });

        Thread thread =
                new Thread(task);

        thread.setDaemon(true);

        thread.start();
    }

    // ================================================================
    // CREATE WISH CARD
    // ================================================================

    private VBox createWishCard(
            WishItem wish
    ) {

        VBox card =
                new VBox(10);

        card.setPadding(
                new Insets(15)
        );

        card.setPrefWidth(250);

        card.setStyle(
                "-fx-border-color: #cccccc;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;"
        );

        Label name =
                new Label(
                        wish.getItemName()
                );

        name.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;"
        );

        Label price =
                new Label(
                        "Price: $" +
                                wish.getItemPrice()
                );

        Label raised =
                new Label(
                        "Raised: $" +
                                wish.getAmountRaised()
                );

        ProgressBar progress =
                new ProgressBar();

        double progressValue = 0;

        if (wish.getItemPrice() != null &&
                wish.getItemPrice().doubleValue() > 0) {

            progressValue =
                    wish.getAmountRaised()
                            .doubleValue()
                            /
                            wish.getItemPrice()
                                    .doubleValue();
        }

        progressValue =
                Math.min(
                        Math.max(
                                progressValue,
                                0
                        ),
                        1
                );

        progress.setProgress(
                progressValue
        );

        Label status;

        if (wish.isComplete()) {

            status =
                    new Label(
                            "✓ Completed"
                    );

            status.setStyle(
                    "-fx-font-weight: bold;"
            );

        } else {

            status =
                    new Label(
                            "Not Completed"
                    );
        }

        card.getChildren().addAll(
                name,
                price,
                raised,
                progress,
                status
        );

        return card;
    }

    // ================================================================
    // CREATE WISH FROM RESPONSE
    // ================================================================

    private WishItem createWishFromResponse(
            Map<?, ?> map
    ) {

        int wishId =
                numberValue(
                        map.get("wish_id")
                );

        int itemId =
                numberValue(
                        map.get("item_id")
                );

        BigDecimal amountRaised =
                decimalValue(
                        map.get("amount_raised")
                );

        boolean complete =
                Boolean.TRUE.equals(
                        map.get("is_complete")
                );

        WishItem wish =
                new WishItem(
                        wishId,
                        currentUserId,
                        itemId,
                        amountRaised,
                        complete
                );

        String name =
                String.valueOf(
                        map.get("name")
                );

        BigDecimal price =
                decimalValue(
                        map.get("price")
                );

        wish.setItemName(name);

        wish.setItemPrice(price);

        return wish;
    }

    // ================================================================
    // WISH LIST CELL
    // ================================================================

    private ListCell<WishItem> createWishListCell() {

        return new ListCell<>() {

            @Override
            protected void updateItem(
                    WishItem item,
                    boolean empty
            ) {

                super.updateItem(
                        item,
                        empty
                );

                if (empty || item == null) {

                    setText(null);

                } else {

                    setText(
                            item.getItemName()
                    );
                }
            }
        };
    }

    // ================================================================
    // LOADING HELPERS
    // ================================================================

    private void setLoading(
            boolean loading,
            Button button,
            ComboBox<?> box,
            ProgressIndicator indicator,
            Label message,
            String text
    ) {

        button.setDisable(loading);

        box.setDisable(loading);

        indicator.setVisible(loading);

        message.setText(text);
    }

    private void setLoading(
            boolean loading,
            Button button,
            ComboBox<?> box1,
            ComboBox<?> box2,
            ProgressIndicator indicator,
            Label message,
            String text
    ) {

        button.setDisable(loading);

        box1.setDisable(loading);

        box2.setDisable(loading);

        indicator.setVisible(loading);

        message.setText(text);
    }

    // ================================================================
    // NUMBER HELPERS
    // ================================================================

    private int numberValue(
            Object value
    ) {

        if (value instanceof Number number) {

            return number.intValue();
        }

        return Integer.parseInt(
                value.toString()
        );
    }

    private BigDecimal decimalValue(
            Object value
    ) {

        if (value instanceof BigDecimal decimal) {

            return decimal;
        }

        return new BigDecimal(
                value.toString()
        );
    }

    // ================================================================
    // ERROR
    // ================================================================

    private void showError(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle("Error");

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }

    // ================================================================
    // MAIN
    // ================================================================

    public static void main(
            String[] args
    ) {

        launch(args);
    }
}

