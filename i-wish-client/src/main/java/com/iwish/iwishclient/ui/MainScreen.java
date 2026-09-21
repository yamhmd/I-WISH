package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.session.Session;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

public class MainScreen extends BorderPane {

    public MainScreen() {
        Label header = new Label("i-Wish  |   Hi, " + Session.get().getCurrentUser().getUsername());
        header.setMaxWidth(Double.MAX_VALUE);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setStyle("-fx-background-color: #6a2c91; -fx-text-fill: white; "
                + "-fx-font-size: 16px; -fx-font-weight: bold;");
        setTop(header);

        FriendsListScreen friendsScreen = new FriendsListScreen();
        AddRemoveFriendScreen manageScreen = new AddRemoveFriendScreen();
        FriendRequestsScreen requestsScreen = new FriendRequestsScreen();

        Tab friendsTab = new Tab("Friends", friendsScreen);
        friendsTab.setClosable(false);
        Tab manageTab = new Tab("Add / Remove Friend", manageScreen);
        manageTab.setClosable(false);
        Tab requestsTab = new Tab("Requests", requestsScreen);
        requestsTab.setClosable(false);

        TabPane tabs = new TabPane(friendsTab, manageTab, requestsTab);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == friendsTab) {
                friendsScreen.refresh();
            } else if (newTab == manageTab) {
                manageScreen.refresh();
            } else if (newTab == requestsTab) {
                requestsScreen.refresh();
            }
        });
        setCenter(tabs);
    }
}
