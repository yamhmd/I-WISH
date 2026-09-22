package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.model.CatalogItem;
import com.iwish.iwishclient.model.WishItem;
import com.iwish.iwishclient.session.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Person 5's "own wish list" screen: add an item from the catalog, change
 * which catalog item an entry points to, or delete it. Contributed items are
 * locked per PROTOCOL.md #11/#12 and shown that way instead of failing silently.
 */
public class WishListScreen extends VBox {

    private final ComboBox<CatalogItem> catalogPicker = new ComboBox<>();
    private final Button addButton = UiStyle.primaryButton("Add to wish list");
    private final ListView<WishItem> listView = new ListView<>();
    private final Button refreshButton = UiStyle.primaryButton("Refresh");

    private List<CatalogItem> catalog = List.of();

    public WishListScreen() {
        setSpacing(UiStyle.SPACING);
        setPadding(UiStyle.SCREEN_PADDING);
        setStyle(UiStyle.screenBackground());

        Label title = UiStyle.title("My Wish List");

        catalogPicker.setPromptText("Choose an item from the catalog…");
        catalogPicker.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(catalogPicker, Priority.ALWAYS);
        addButton.setOnAction(e -> addSelected());

        HBox addRow = new HBox(UiStyle.SPACING, catalogPicker, addButton);

        listView.setPlaceholder(new Label("Your wish list is empty. Add something above!"));
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.setCellFactory(lv -> new WishItemCell());

        refreshButton.setOnAction(e -> refresh());
        HBox.setHgrow(refreshButton, Priority.NEVER);

        getChildren().addAll(title, addRow, listView, refreshButton);
        loadCatalog();
        refresh();
    }

    public void refresh() {
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runAsync(
                () -> Session.get().getApi().viewMyWishlist(userId),
                items -> listView.getItems().setAll(items),
                listView, refreshButton, addButton
        );
    }

    private void loadCatalog() {
        UiHelper.runAsync(
                () -> Session.get().getApi().viewCatalog(),
                items -> {
                    catalog = items;
                    catalogPicker.getItems().setAll(items);
                },
                catalogPicker, addButton
        );
    }

    private void addSelected() {
        CatalogItem chosen = catalogPicker.getValue();
        if (chosen == null) {
            UiHelper.showError("Please choose an item from the catalog first.");
            return;
        }
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runVoid(
                () -> Session.get().getApi().createWishItem(userId, chosen.getItemId()),
                () -> {
                    UiHelper.showInfo("Added \"" + chosen.getName() + "\" to your wish list.");
                    catalogPicker.setValue(null);
                    refresh();
                },
                addButton, catalogPicker
        );
    }

    private class WishItemCell extends ListCell<WishItem> {
        private final Label name = new Label();
        private final Label progressLabel = UiStyle.muted("");
        private final ProgressBar progressBar = new ProgressBar(0);
        private final Region spacer = new Region();
        private final Button changeButton = UiStyle.secondaryButton("Change item");
        private final Button deleteButton = UiStyle.dangerButton("Delete");
        private final Label lockedLabel = UiStyle.muted("🔒 Has contributions — locked");
        private final VBox left = new VBox(2, name, new HBox(UiStyle.SPACING_SMALL, progressBar, progressLabel));
        private final HBox row = new HBox(UiStyle.SPACING, left, spacer, changeButton, deleteButton, lockedLabel);

        WishItemCell() {
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 0, 6, 0));
            progressBar.setPrefWidth(120);
            name.setStyle("-fx-font-weight: bold;");
            changeButton.setOnAction(e -> changeItem(getItem()));
            deleteButton.setOnAction(e -> confirmDelete(getItem()));
        }

        @Override
        protected void updateItem(WishItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            String status = item.isComplete() ? "  ✅ Complete" : "";
            name.setText(item.getName() + status);
            progressBar.setProgress(item.getPrice() <= 0 ? 0 : item.getAmountRaised() / item.getPrice());
            progressLabel.setText(String.format("%.2f / %.2f EGP", item.getAmountRaised(), item.getPrice()));

            boolean locked = item.hasContributions();
            changeButton.setVisible(!locked);
            changeButton.setManaged(!locked);
            deleteButton.setVisible(!locked);
            deleteButton.setManaged(!locked);
            lockedLabel.setVisible(locked);
            lockedLabel.setManaged(locked);

            setGraphic(row);
        }
    }

    private void changeItem(WishItem item) {
        if (item == null) return;
        ComboBox<CatalogItem> picker = new ComboBox<>();
        picker.getItems().setAll(catalog);
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Change item");
        dialog.setHeaderText("Swap \"" + item.getName() + "\" for a different catalog item");
        dialog.getDialogPane().setContent(picker);
        dialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        CatalogItem chosen = picker.getValue();
        if (chosen == null) {
            UiHelper.showError("Please choose a replacement item.");
            return;
        }
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runVoid(
                () -> Session.get().getApi().updateWishItem(userId, item.getWishId(), chosen.getItemId()),
                this::refresh,
                listView
        );
    }

    private void confirmDelete(WishItem item) {
        if (item == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove \"" + item.getName() + "\" from your wish list?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete wish item");
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }
        int userId = Session.get().getCurrentUser().getUserId();
        UiHelper.runVoid(
                () -> Session.get().getApi().deleteWishItem(userId, item.getWishId()),
                this::refresh,
                listView
        );
    }
}
