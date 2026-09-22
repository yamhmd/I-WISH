package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.model.User;
import com.iwish.iwishclient.model.WishItem;
import com.iwish.iwishclient.session.Session;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * TASK_ASSIGNMENTS, Person 6, items 2 & 4: pick a friend's item, enter an
 * amount, confirm — wired to PROTOCOL.md #15 CONTRIBUTE, with the two
 * documented error cases surfaced as real UI feedback rather than silent
 * failures ("Amount exceeds remaining price", "Cannot contribute to your own
 * wish item"), plus "Not friends" and "Item already complete" for safety.
 */
public class ContributeScreen extends VBox {

    private final ComboBox<User> friendPicker = new ComboBox<>();
    private final ComboBox<WishItem> itemPicker = new ComboBox<>();
    private final Label remainingLabel = UiStyle.muted("");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final TextField amountField = new TextField();
    private final javafx.scene.control.Button contributeButton = UiStyle.primaryButton("Contribute");

    public ContributeScreen() {
        setSpacing(UiStyle.SPACING);
        setPadding(UiStyle.SCREEN_PADDING);
        setStyle(UiStyle.screenBackground());

        Label title = UiStyle.title("Contribute to a Friend's Wish");

        Label friendLabel = sectionLabel("1. Choose a friend");
        friendPicker.setPromptText("Choose a friend…");
        friendPicker.setMaxWidth(Double.MAX_VALUE);
        friendPicker.setOnAction(e -> onFriendSelected());

        Label itemLabel = sectionLabel("2. Choose an item");
        itemPicker.setPromptText("Choose one of their wish items…");
        itemPicker.setMaxWidth(Double.MAX_VALUE);
        itemPicker.setDisable(true);
        itemPicker.setCellFactory(lv -> new WishItemListCell());
        itemPicker.setButtonCell(new WishItemListCell());
        itemPicker.setOnAction(e -> onItemSelected());

        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        Label amountLabel = sectionLabel("3. Enter an amount (EGP)");
        amountField.setPromptText("e.g. 100.00");
        amountField.setMaxWidth(220);
        amountField.setDisable(true);
        amountField.setOnAction(e -> submit());

        contributeButton.setDisable(true);
        contributeButton.setOnAction(e -> submit());

        VBox card = new VBox(UiStyle.SPACING,
                friendLabel, friendPicker,
                itemLabel, itemPicker, progressBar, remainingLabel,
                amountLabel, amountField,
                contributeButton
        );
        card.setPadding(new Insets(UiStyle.SPACING_LARGE));
        card.setStyle(UiStyle.cardStyle());
        card.setMaxWidth(480);

        getChildren().addAll(title, card);
        loadFriends();
    }

    public void refresh() {
        loadFriends();
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

    private void onFriendSelected() {
        User friend = friendPicker.getValue();
        resetItemSelection();
        if (friend == null) {
            return;
        }
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runAsync(
                () -> Session.get().getApi().viewFriendWishlist(userId, friend.getUserId()),
                wishlist -> {
                    itemPicker.getItems().setAll(wishlist.getWishItems());
                    itemPicker.setDisable(wishlist.getWishItems().isEmpty());
                    if (wishlist.getWishItems().isEmpty()) {
                        UiHelper.showInfo(friend.getUsername() + " doesn't have any wish list items yet.");
                    }
                },
                friendPicker, itemPicker
        );
    }

    private void onItemSelected() {
        WishItem item = itemPicker.getValue();
        if (item == null) {
            resetItemSelection();
            return;
        }
        progressBar.setProgress(item.getPrice() <= 0 ? 0 : item.getAmountRaised() / item.getPrice());
        remainingLabel.setText(item.isComplete()
                ? "This item is already fully funded."
                : String.format("Raised %.2f / %.2f EGP — %.2f EGP remaining",
                        item.getAmountRaised(), item.getPrice(), item.getRemaining()));
        boolean canContribute = !item.isComplete();
        amountField.setDisable(!canContribute);
        contributeButton.setDisable(!canContribute);
        if (!canContribute) {
            amountField.clear();
        }
    }

    private void resetItemSelection() {
        itemPicker.getItems().clear();
        itemPicker.setValue(null);
        itemPicker.setDisable(true);
        progressBar.setProgress(0);
        remainingLabel.setText("");
        amountField.clear();
        amountField.setDisable(true);
        contributeButton.setDisable(true);
    }

    private void submit() {
        WishItem item = itemPicker.getValue();
        if (item == null) {
            UiHelper.showError("Please choose an item to contribute to.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException ex) {
            UiHelper.showError("Please enter a valid amount.");
            return;
        }
        if (amount <= 0) {
            UiHelper.showError("Amount must be greater than zero.");
            return;
        }

        int userId = Session.get().getCurrentUser().getUserId();
        int wishId = item.getWishId();
        Node[] controls = {friendPicker, itemPicker, amountField, contributeButton};

        UiHelper.runAsync(
                () -> Session.get().getApi().contribute(userId, wishId, amount),
                completed -> {
                    UiHelper.showInfo(completed
                            ? "Contribution recorded — that fully funded this item! 🎉"
                            : "Contribution recorded. Thank you!");
                    amountField.clear();
                    onFriendSelected(); // reload the friend's items so progress reflects the new total
                },
                controls
        );
    }

    private static Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: " + UiStyle.BODY_SIZE + "; -fx-font-weight: bold; -fx-text-fill: "
                + UiStyle.PRIMARY_DARK + ";");
        return label;
    }

    /** Shared render for the item combo box's popup rows and its closed-state button cell. */
    private static class WishItemListCell extends ListCell<WishItem> {
        @Override
        protected void updateItem(WishItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                return;
            }
            String status = item.isComplete() ? " (fully funded)" : "";
            setText(String.format("%s — %.2f/%.2f EGP%s",
                    item.getName(), item.getAmountRaised(), item.getPrice(), status));
        }
    }
}
