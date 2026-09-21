package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.model.User;
import com.iwish.iwishclient.session.Session;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class FriendsListScreen extends VBox {

    private final ListView<User> listView = new ListView<>();
    private final Button refreshButton = new Button("Refresh");

    public FriendsListScreen() {
        setSpacing(12);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #fdf6ff;");

        Label title = new Label("My Friends");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #6a2c91;");

        listView.setPlaceholder(new Label("You have no friends yet."));
        VBox.setVgrow(listView, Priority.ALWAYS);

        refreshButton.setStyle("-fx-background-color: #6a2c91; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8;");
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
