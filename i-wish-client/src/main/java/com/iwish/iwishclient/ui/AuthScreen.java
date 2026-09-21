package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.session.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class AuthScreen extends VBox {

    private final Label titleLabel = new Label("i-Wish");
    private final Label subtitleLabel = new Label();
    private final TextField usernameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button submitButton = UiStyle.primaryButton("");
    private final Hyperlink switchLink = new Hyperlink();

    private final Runnable onSuccess;
    private boolean registerMode = false;

    public AuthScreen(Runnable onSuccess) {
        this.onSuccess = onSuccess;

        setSpacing(UiStyle.SPACING);
        setPadding(new Insets(30));
        setAlignment(Pos.CENTER);
        setStyle(UiStyle.screenBackground());

        titleLabel.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: " + UiStyle.PRIMARY + ";");
        subtitleLabel.setStyle("-fx-font-size: " + UiStyle.BODY_SIZE + "; -fx-text-fill: " + UiStyle.MUTED_TEXT + ";");

        usernameField.setPromptText("Username");
        emailField.setPromptText("Email");
        passwordField.setPromptText("Password");
        usernameField.setMaxWidth(300);
        emailField.setMaxWidth(300);
        passwordField.setMaxWidth(300);

        submitButton.setMaxWidth(300);

        submitButton.setOnAction(e -> submit());
        passwordField.setOnAction(e -> submit());
        switchLink.setOnAction(e -> {
            registerMode = !registerMode;
            refreshMode();
        });

        getChildren().addAll(titleLabel, subtitleLabel, usernameField, emailField,
                passwordField, submitButton, switchLink);
        refreshMode();
    }

    private void refreshMode() {
        emailField.setVisible(registerMode);
        emailField.setManaged(registerMode);
        subtitleLabel.setText(registerMode ? "Create your account" : "Sign in to continue");
        submitButton.setText(registerMode ? "Register" : "Sign in");
        switchLink.setText(registerMode ? "Already have an account? Sign in" : "New here? Create an account");
    }

    private void submit() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty() || (registerMode && email.isEmpty())) {
            UiHelper.showError("Please fill in all fields.");
            return;
        }
        if (registerMode && !email.contains("@")) {
            UiHelper.showError("Please enter a valid email address.");
            return;
        }

        Node[] controls = {submitButton, switchLink, usernameField, emailField, passwordField};

        UiHelper.runAsync(
                () -> registerMode
                        ? Session.get().getApi().register(username, email, password)
                        : Session.get().getApi().login(username, password),
                user -> {
                    Session.get().setCurrentUser(user);
                    onSuccess.run();
                },
                controls
        );
    }
}
