package iwish.ui;
import iwish.db.WishItemDAO;
import iwish.model.WishItem;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class DeleteWishItemApp extends Application {

    private final WishItemDAO wishItemDAO = new WishItemDAO();

    // مؤقت لحد ما نربطه بالـ Login
    private final int currentUserId = 1;

    @Override
    public void start(Stage stage) {

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label title = new Label("Delete Wish Item");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        ComboBox<WishItem> wishBox =
                new ComboBox<>();

        wishBox.setPromptText(
                "Select Wish Item"
        );

        Button deleteButton =
                new Button("Delete");

        // مفيش Delete قبل اختيار عنصر
        deleteButton.setDisable(true);

        Label message =
                new Label();

        // لما المستخدم يختار عنصر
        wishBox.setOnAction(event -> {

            deleteButton.setDisable(
                    wishBox.getValue() == null
            );

            message.setText("");
        });

        // Load wishlist
        try {

            List<WishItem> wishes =
                    wishItemDAO.getWishItemsByUser(
                            currentUserId
                    );

            wishBox.getItems().addAll(wishes);

        } catch (Exception e) {

            message.setText(
                    "Error loading wishlist: " +
                            e.getMessage()
            );
        }

        // Display wish item name
        wishBox.setCellFactory(
                list -> new ListCell<>() {

                    @Override
                    protected void updateItem(
                            WishItem item,
                            boolean empty) {

                        super.updateItem(
                                item,
                                empty
                        );

                        if (empty || item == null) {

                            setText(null);

                        } else {

                            setText(
                                    item.getItemName()
                            );
                        }
                    }
                }
        );

        wishBox.setButtonCell(
                new ListCell<>() {

                    @Override
                    protected void updateItem(
                            WishItem item,
                            boolean empty) {

                        super.updateItem(
                                item,
                                empty
                        );

                        if (empty || item == null) {

                            setText(null);

                        } else {

                            setText(
                                    item.getItemName()
                            );
                        }
                    }
                }
        );

        // Delete button
        deleteButton.setOnAction(event -> {

            WishItem selectedWish =
                    wishBox.getValue();

            if (selectedWish == null) {

                message.setText(
                        "Please select a wish item."
                );

                return;
            }

            // Confirmation
            Alert confirmation =
                    new Alert(
                            Alert.AlertType.CONFIRMATION
                    );

            confirmation.setTitle(
                    "Delete Wish Item"
            );

            confirmation.setHeaderText(
                    "Delete " +
                            selectedWish.getItemName() +
                            "?"
            );

            confirmation.setContentText(
                    "Are you sure you want to delete this wish item?"
            );

            ButtonType result =
                    confirmation
                            .showAndWait()
                            .orElse(
                                    ButtonType.CANCEL
                            );

            if (result != ButtonType.OK) {
                return;
            }

            // Loading state
            deleteButton.setDisable(true);
            deleteButton.setText("Deleting...");
            message.setText("");

            try {

                wishItemDAO.deleteWishItem(
                        currentUserId,
                        selectedWish.getWishId()
                );

                // Remove it from ComboBox
                wishBox.getItems().remove(
                        selectedWish
                );

                wishBox.setValue(null);

                message.setText(
                        "Wish item deleted successfully!"
                );

            } catch (IllegalStateException e) {

                message.setText(
                        e.getMessage()
                );

                deleteButton.setDisable(false);

            } catch (Exception e) {

                message.setText(
                        "Error: " +
                                e.getMessage()
                );

                deleteButton.setDisable(false);

            } finally {

                deleteButton.setText("Delete");

                if (wishBox.getValue() != null) {
                    deleteButton.setDisable(false);
                }
            }
        });

        root.getChildren().addAll(
                title,
                new Label("Choose Wish Item:"),
                wishBox,
                deleteButton,
                message
        );

        Scene scene =
                new Scene(
                        root,
                        450,
                        300
                );

        stage.setTitle(
                "Delete Wish Item"
        );

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {

        launch(args);
    }
}

