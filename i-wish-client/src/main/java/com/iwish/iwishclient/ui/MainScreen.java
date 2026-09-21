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
        header.setStyle("-fx-background-color: " + UiStyle.PRIMARY + "; -fx-text-fill: white; "
                + "-fx-font-size: 16px; -fx-font-weight: bold;");
        setTop(header);

        FriendsListScreen friendsScreen = new FriendsListScreen();
        AddRemoveFriendScreen manageScreen = new AddRemoveFriendScreen();
        FriendRequestsScreen requestsScreen = new FriendRequestsScreen();
        WishListScreen wishListScreen = new WishListScreen();
        FriendWishListScreen friendWishListScreen = new FriendWishListScreen();
        ContributeScreen contributeScreen = new ContributeScreen();
        NotificationsScreen notificationsScreen = new NotificationsScreen();

        Tab friendsTab = tab("Friends", friendsScreen);
        Tab manageTab = tab("Add / Remove Friend", manageScreen);
        Tab requestsTab = tab("Requests", requestsScreen);
        Tab myWishTab = tab("My Wish List", wishListScreen);
        Tab friendWishTab = tab("Friend's Wish List", friendWishListScreen);
        Tab contributeTab = tab("Contribute", contributeScreen);
        Tab notifTab = tab("Notifications", notificationsScreen);

        TabPane tabs = new TabPane(
                friendsTab, manageTab, requestsTab,
                myWishTab, friendWishTab, contributeTab, notifTab
        );
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == friendsTab) friendsScreen.refresh();
            else if (newTab == manageTab) manageScreen.refresh();
            else if (newTab == requestsTab) requestsScreen.refresh();
            else if (newTab == myWishTab) wishListScreen.refresh();
            else if (newTab == friendWishTab) friendWishListScreen.refresh();
            else if (newTab == contributeTab) contributeScreen.refresh();
            else if (newTab == notifTab) notificationsScreen.refresh();
        });
        setCenter(tabs);
    }

    private static Tab tab(String title, javafx.scene.Node content) {
        Tab t = new Tab(title, content);
        t.setClosable(false);
        return t;
    }
}
