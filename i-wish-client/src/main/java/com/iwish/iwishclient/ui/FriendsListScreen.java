package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.model.User;
import com.iwish.iwishclient.session.Session;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class FriendsListScreen extends VBox {

    private final ListView<User> listView = new ListView<>();
    private final Button refreshButton = UiStyle.primaryButton("Refresh");

    public FriendsListScreen() {
        setSpacing(UiStyle.SPACING);
        setPadding(UiStyle.SCREEN_PADDING);
        setStyle(UiStyle.screenBackground());

        Label title = UiStyle.title("My Friends");

        listView.setPlaceholder(new Label("You have no friends yet."));
        VBox.setVgrow(listView, Priority.ALWAYS);

        refreshButton.setOnAction(e -> refresh());

        getChildren().addAll(title, listView, refreshButton);
        refresh();
    }

    public void refresh() {
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runAsync(
                () -> Session.get().getApi().viewFriends(userId),
                friends -> listView.getItems().setAll(friends),
                refreshButton
        );
    }
}
