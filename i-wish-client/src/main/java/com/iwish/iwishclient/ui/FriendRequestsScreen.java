package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.model.FriendRequest;
import com.iwish.iwishclient.session.Session;
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
    private final Button refreshButton = UiStyle.primaryButton("Refresh");

    public FriendRequestsScreen() {
        setSpacing(UiStyle.SPACING);
        setPadding(UiStyle.SCREEN_PADDING);
        setStyle(UiStyle.screenBackground());

        Label title = UiStyle.title("Friend requests");

        listView.setPlaceholder(new Label("No pending requests."));
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.setCellFactory(lv -> new ListCell<>() {
            private final Label name = new Label();
            private final Region spacer = new Region();
            private final Button acceptButton = UiStyle.successButton("Accept");
            private final Button declineButton = UiStyle.dangerButton("Decline");
            private final HBox row = new HBox(UiStyle.SPACING, name, spacer, acceptButton, declineButton);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);
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
