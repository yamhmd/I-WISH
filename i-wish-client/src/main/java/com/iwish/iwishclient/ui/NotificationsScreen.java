package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.model.NotificationItem;
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

import java.util.List;

/**
 * TASK_ASSIGNMENTS, Person 6, items 3 & 5: notifications inbox — list of
 * messages with an unread indicator, wired to PROTOCOL.md #16
 * GET_NOTIFICATIONS and #17 MARK_NOTIFICATION_READ.
 */
public class NotificationsScreen extends VBox {

    private final Label unreadSummary = UiStyle.muted("");
    private final ListView<NotificationItem> listView = new ListView<>();
    private final Button refreshButton = UiStyle.primaryButton("Refresh");
    private final Button markAllReadButton = UiStyle.secondaryButton("Mark all as read");

    public NotificationsScreen() {
        setSpacing(UiStyle.SPACING);
        setPadding(UiStyle.SCREEN_PADDING);
        setStyle(UiStyle.screenBackground());

        Label title = UiStyle.title("Notifications");

        HBox headerRow = new HBox(UiStyle.SPACING, unreadSummary);
        HBox.setHgrow(unreadSummary, Priority.ALWAYS);

        listView.setPlaceholder(new Label("No notifications yet."));
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.setCellFactory(lv -> new NotificationCell());

        refreshButton.setOnAction(e -> refresh());
        markAllReadButton.setOnAction(e -> markAllRead());
        HBox actionsRow = new HBox(UiStyle.SPACING, refreshButton, markAllReadButton);

        getChildren().addAll(title, headerRow, listView, actionsRow);
        refresh();
    }

    public void refresh() {
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runAsync(
                () -> Session.get().getApi().getNotifications(userId),
                notifications -> {
                    // Unread first, then newest first, so the things that need
                    // attention are never buried below old read notifications.
                    List<NotificationItem> sorted = notifications.stream()
                            .sorted((a, b) -> {
                                if (a.isRead() != b.isRead()) return a.isRead() ? 1 : -1;
                                return b.getCreatedAt().compareTo(a.getCreatedAt());
                            })
                            .toList();
                    listView.getItems().setAll(sorted);
                    long unread = notifications.stream().filter(n -> !n.isRead()).count();
                    unreadSummary.setText(unread == 0 ? "You're all caught up."
                            : unread + " unread notification" + (unread == 1 ? "" : "s"));
                    markAllReadButton.setDisable(unread == 0);
                },
                refreshButton, markAllReadButton
        );
    }

    private void markRead(NotificationItem notification) {
        if (notification == null || notification.isRead()) {
            return;
        }
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runVoid(
                () -> Session.get().getApi().markNotificationRead(userId, notification.getNotifId()),
                this::refresh,
                listView
        );
    }

    private void markAllRead() {
        int userId = Session.get().getCurrentUser().getUserId();
        List<NotificationItem> unread = listView.getItems().stream().filter(n -> !n.isRead()).toList();
        if (unread.isEmpty()) {
            return;
        }
        UiHelper.runVoid(
                () -> {
                    for (NotificationItem n : unread) {
                        Session.get().getApi().markNotificationRead(userId, n.getNotifId());
                    }
                },
                this::refresh,
                listView, refreshButton, markAllReadButton
        );
    }

    private class NotificationCell extends ListCell<NotificationItem> {
        private final Label unreadDot = new Label("●");
        private final Label typeBadge = new Label();
        private final Label message = new Label();
        private final Label timestamp = UiStyle.muted("");
        private final Region spacer = new Region();
        private final Button markReadButton = UiStyle.secondaryButton("Mark read");
        private final VBox textColumn = new VBox(2, message, timestamp);
        private final HBox row = new HBox(UiStyle.SPACING_SMALL,
                unreadDot, typeBadge, textColumn, spacer, markReadButton);

        NotificationCell() {
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 0, 6, 0));
            unreadDot.setStyle("-fx-text-fill: " + UiStyle.PRIMARY + "; -fx-font-size: 10px;");
            message.setWrapText(true);
            markReadButton.setOnAction(e -> markRead(getItem()));
        }

        @Override
        protected void updateItem(NotificationItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            boolean unread = !item.isRead();
            unreadDot.setVisible(unread);
            unreadDot.setManaged(unread);

            boolean isReceiver = "receiver".equalsIgnoreCase(item.getType());
            typeBadge.setText(isReceiver ? "🎁 Received" : "💜 Contributed");
            typeBadge.setStyle("-fx-font-size: " + UiStyle.SMALL_SIZE + "; -fx-font-weight: bold; -fx-text-fill: "
                    + (isReceiver ? UiStyle.SUCCESS : UiStyle.PRIMARY) + ";");

            message.setText(item.getMessage());
            message.setStyle(unread ? "-fx-font-weight: bold;" : "-fx-font-weight: normal; -fx-text-fill: "
                    + UiStyle.MUTED_TEXT + ";");
            timestamp.setText(item.getCreatedAt());

            markReadButton.setVisible(unread);
            markReadButton.setManaged(unread);

            setGraphic(row);
        }
    }
}
