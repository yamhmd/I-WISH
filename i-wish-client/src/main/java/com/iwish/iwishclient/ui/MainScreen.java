package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.session.Session;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class MainScreen extends BorderPane {

    private final Runnable onLogout;

    public MainScreen(Runnable onLogout) {
        this.onLogout = onLogout;

        // Header
        Label header = new Label(
                "i-Wish  |   Hi, " +
                        Session.get().getCurrentUser().getUsername()
        );

        header.setPadding(new Insets(12, 20, 12, 20));

        header.setStyle(
                "-fx-background-color: #6a2c91; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold;"
        );

        // Logout button
        Button logoutButton = new Button("Logout");

        logoutButton.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: #6a2c91; " +
                        "-fx-font-weight: bold;"
        );

        logoutButton.setOnAction(e -> onLogout.run());

        // Header layout
        HBox headerBox = new HBox();

        headerBox.setStyle(
                "-fx-background-color: #6a2c91;"
        );

        headerBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        headerBox.getChildren().addAll(
                header,
                spacer,
                logoutButton
        );

        setTop(headerBox);

        // Screens
        FriendsListScreen friendsScreen =
                new FriendsListScreen();

        AddRemoveFriendScreen manageScreen =
                new AddRemoveFriendScreen();

        FriendRequestsScreen requestsScreen =
                new FriendRequestsScreen();

        WishListScreen wishListScreen =
                new WishListScreen();

        FriendWishListScreen friendWishListScreen =
                new FriendWishListScreen();

        ContributeScreen contributeScreen =
                new ContributeScreen();

        NotificationsScreen notificationsScreen =
                new NotificationsScreen();

        // Tabs
        Tab friendsTab =
                tab("Friends", friendsScreen);

        Tab manageTab =
                tab("Add / Remove Friend", manageScreen);

        Tab requestsTab =
                tab("Requests", requestsScreen);

        Tab myWishTab =
                tab("My Wish List", wishListScreen);

        Tab friendWishTab =
                tab("Friend's Wish List", friendWishListScreen);

        Tab contributeTab =
                tab("Contribute", contributeScreen);

        Tab notifTab =
                tab("Notifications", notificationsScreen);

        // TabPane
        TabPane tabs = new TabPane(
                friendsTab,
                manageTab,
                requestsTab,
                myWishTab,
                friendWishTab,
                contributeTab,
                notifTab
        );

        // Refresh when changing tabs
        tabs.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldTab, newTab) -> {

                            if (newTab == friendsTab) {
                                friendsScreen.refresh();

                            } else if (newTab == manageTab) {
                                manageScreen.refresh();

                            } else if (newTab == requestsTab) {
                                requestsScreen.refresh();

                            } else if (newTab == myWishTab) {
                                wishListScreen.refresh();

                            } else if (newTab == friendWishTab) {
                                friendWishListScreen.refresh();

                            } else if (newTab == contributeTab) {
                                contributeScreen.refresh();

                            } else if (newTab == notifTab) {
                                notificationsScreen.refresh();
                            }
                        }
                );

        setCenter(tabs);
    }

    private static Tab tab(
            String title,
            javafx.scene.Node content
    ) {
        Tab t = new Tab(title, content);
        t.setClosable(false);
        return t;
    }

    private void logout() {
        onLogout.run();
    }
}