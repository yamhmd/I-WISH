package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.model.FriendRequest;
import com.iwish.iwishclient.session.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class FriendRequestsScreen extends VBox {

    private final ListView<FriendRequest> listView = new ListView<>();
    private final Button refreshButton = new Button("Refresh");

    public FriendRequestsScreen() {
        setSpacing(12);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: #fdf6ff;");

        Label title = new Label("Friend requests");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #6a2c91;");

        listView.setPlaceholder(new Label("No pending requests."));
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.setCellFactory(lv -> new ListCell<>() {
            private final Label name = new Label();
            private final Region spacer = new Region();
            private final Button acceptButton = new Button("Accept");
            private final Button declineButton = new Button("Decline");
            private final HBox row = new HBox(10, name, spacer, acceptButton, declineButton);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);
                acceptButton.setStyle("-fx-background-color: #5cb85c; -fx-text-fill: white; "
                        + "-fx-background-radius: 6;");
                declineButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; "
                        + "-fx-background-radius: 6;");
                acceptButton.setOnAction(e -> respond(getItem(), true));
                declineButton.setOnAction(e -> respond(getItem(), false));
            }

            @Override
            protected void updateItem(FriendRequest request, boolean empty) {
                super.updateItem(request, empty);
                if (empty || request == null) {
                    setGraphic(null);
                } else {
                    name.setText(request.getUsername() + " wants to be your friend");
                    setGraphic(row);
                }
            }
        });

        refreshButton.setStyle("-fx-background-color: #6a2c91; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8;");
        refreshButton.setOnAction(e -> refresh());

        getChildren().addAll(title, listView, refreshButton);
        refresh();
    }

    public void refresh() {
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runAsync(
                () -> Session.get().getApi().viewPendingRequests(userId),
                requests -> listView.getItems().setAll(requests),
                refreshButton
        );
    }

    private void respond(FriendRequest request, boolean accept) {
        if (request == null) {
            return;
        }
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runVoid(
                () -> {
                    if (accept) {
                        Session.get().getApi().acceptFriend(userId, request.getRequesterId());
                    } else {
                        Session.get().getApi().declineFriend(userId, request.getRequesterId());
                    }
                },
                this::refresh,
                listView, refreshButton
        );
    }
}
