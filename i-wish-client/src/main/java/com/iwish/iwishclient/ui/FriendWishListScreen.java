package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.model.User;
import com.iwish.iwishclient.model.WishItem;
import com.iwish.iwishclient.session.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Person 5's read-only view of a friend's wish list: pick a friend, see each
 * item's progress (PROTOCOL.md #14 VIEW_FRIEND_WISHLIST), completed items
 * marked visibly.
 */
public class FriendWishListScreen extends VBox {

    private final ComboBox<User> friendPicker = new ComboBox<>();
    private final Button loadButton = UiStyle.primaryButton("View wish list");
    private final Label ownerLabel = UiStyle.subtitle("");
    private final ListView<WishItem> listView = new ListView<>();

    public FriendWishListScreen() {
        setSpacing(UiStyle.SPACING);
        setPadding(UiStyle.SCREEN_PADDING);
        setStyle(UiStyle.screenBackground());

        Label title = UiStyle.title("Friend's Wish List");

        friendPicker.setPromptText("Choose a friend…");
        friendPicker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(friendPicker, Priority.ALWAYS);
        loadButton.setOnAction(e -> loadSelected());

        HBox pickRow = new HBox(UiStyle.SPACING, friendPicker, loadButton);

        listView.setPlaceholder(new Label("Choose a friend above to see their wish list."));
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.setCellFactory(lv -> new FriendWishItemCell());

        getChildren().addAll(title, pickRow, ownerLabel, listView);
        loadFriends();
    }

    public void refresh() {
        loadFriends();
        if (friendPicker.getValue() != null) {
            loadSelected();
        }
    }

    private void loadFriends() {
        int userId = Session.get().getCurrentUser().getUserId();
        User previouslySelected = friendPicker.getValue();
        UiHelper.runAsync(
                () -> Session.get().getApi().viewFriends(userId),
                friends -> {
                    friendPicker.getItems().setAll(friends);
                    if (previouslySelected != null && friends.contains(previouslySelected)) {
                        friendPicker.setValue(previouslySelected);
                    }
                },
                friendPicker
        );
    }

    private void loadSelected() {
        User friend = friendPicker.getValue();
        if (friend == null) {
            UiHelper.showError("Please choose a friend first.");
            return;
        }
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runAsync(
                () -> Session.get().getApi().viewFriendWishlist(userId, friend.getUserId()),
                wishlist -> {
                    ownerLabel.setText(wishlist.getFriendUsername() + "'s items");
                    listView.getItems().setAll(wishlist.getWishItems());
                },
                friendPicker, loadButton
        );
    }

    private static class FriendWishItemCell extends ListCell<WishItem> {
        private final Label name = new Label();
        private final Label progressLabel = UiStyle.muted("");
        private final ProgressBar progressBar = new ProgressBar(0);
        private final Region spacer = new Region();
        private final Label completeBadge = new Label("✅ Fully funded");
        private final VBox left = new VBox(2, name, new HBox(UiStyle.SPACING_SMALL, progressBar, progressLabel));
        private final HBox row = new HBox(UiStyle.SPACING, left, spacer, completeBadge);

        FriendWishItemCell() {
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 0, 6, 0));
            progressBar.setPrefWidth(120);
            name.setStyle("-fx-font-weight: bold;");
            completeBadge.setStyle("-fx-text-fill: " + UiStyle.SUCCESS + "; -fx-font-weight: bold;");
        }

        @Override
        protected void updateItem(WishItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            name.setText(item.getName());
            progressBar.setProgress(item.getPrice() <= 0 ? 0 : item.getAmountRaised() / item.getPrice());
            progressLabel.setText(String.format("%.2f / %.2f EGP", item.getAmountRaised(), item.getPrice()));
            completeBadge.setVisible(item.isComplete());
            completeBadge.setManaged(item.isComplete());
            setGraphic(row);
        }
    }
}
