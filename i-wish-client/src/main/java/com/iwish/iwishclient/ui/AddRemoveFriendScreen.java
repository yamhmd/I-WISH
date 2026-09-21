package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.model.User;
import com.iwish.iwishclient.session.Session;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AddRemoveFriendScreen extends VBox {

    private final TextField usernameField = new TextField();
    private final Button addButton = UiStyle.primaryButton("Send request");
    private final ListView<User> listView = new ListView<>();

    public AddRemoveFriendScreen() {
        setSpacing(UiStyle.SPACING);
        setPadding(UiStyle.SCREEN_PADDING);
        setStyle(UiStyle.screenBackground());

        Label addTitle = UiStyle.subtitle("Add a friend");

        usernameField.setPromptText("Friend's username");
        HBox.setHgrow(usernameField, Priority.ALWAYS);

        addButton.setOnAction(e -> sendRequest());
        usernameField.setOnAction(e -> sendRequest());

        HBox addRow = new HBox(UiStyle.SPACING, usernameField, addButton);

        Label removeTitle = UiStyle.subtitle("My friends");

        listView.setPlaceholder(new Label("You have no friends yet."));
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.setCellFactory(lv -> new ListCell<>() {
            private final Label name = new Label();
            private final Region spacer = new Region();
            private final Button removeButton = UiStyle.dangerButton("Remove");
            private final HBox row = new HBox(UiStyle.SPACING, name, spacer, removeButton);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);
                removeButton.setOnAction(e -> confirmAndRemove(getItem()));
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    name.setText(user.getUsername());
                    setGraphic(row);
                }
            }
        });

        getChildren().addAll(addTitle, addRow, removeTitle, listView);
        refresh();
    }

    public void refresh() {
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runAsync(
                () -> Session.get().getApi().viewFriends(userId),
                friends -> listView.getItems().setAll(friends),
                listView
        );
    }

    private void sendRequest() {
        String friendName = usernameField.getText().trim();
        if (friendName.isEmpty()) {
            UiHelper.showError("Please enter a username.");
            return;
        }
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runVoid(
                () -> Session.get().getApi().addFriend(userId, friendName),
                () -> {
                    UiHelper.showInfo("Friend request sent to " + friendName + ".");
                    usernameField.clear();
                },
                addButton, usernameField
        );
    }

    private void confirmAndRemove(User friend) {
        if (friend == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + friend.getUsername() + " from your friends?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Remove friend");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runVoid(
                () -> Session.get().getApi().removeFriend(userId, friend.getUserId()),
                this::refresh,
                listView
        );
    }
}
