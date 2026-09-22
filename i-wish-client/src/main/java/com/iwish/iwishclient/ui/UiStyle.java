package com.iwish.iwishclient.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * Single source of truth for colors, fonts and spacing across the whole app.
 *
 * Person 6 task 6 ("do a full pass across every screen for consistent
 * colors, fonts, icons, and spacing") is impossible to keep true over time if
 * every screen hardcodes its own copy of "#6a2c91" and "-fx-font-weight:
 * bold". Centralizing the tokens here means a future palette change is a
 * one-file edit, and every new screen automatically matches the rest of the
 * app just by using these helpers instead of inline -fx-* strings.
 */
public final class UiStyle {

    private UiStyle() {}

    // ---- Palette ----
    public static final String PRIMARY = "#6a2c91";
    public static final String PRIMARY_DARK = "#4d1f6b";
    public static final String BACKGROUND = "#fdf6ff";
    public static final String SUCCESS = "#5cb85c";
    public static final String DANGER = "#d9534f";
    public static final String WARNING = "#e6a817";
    public static final String MUTED_TEXT = "#777777";
    public static final String CARD_BACKGROUND = "#ffffff";
    public static final String BORDER = "#e6d6f2";

    // ---- Spacing ----
    public static final int SPACING_SMALL = 8;
    public static final int SPACING = 12;
    public static final int SPACING_LARGE = 20;
    public static final Insets SCREEN_PADDING = new Insets(SPACING_LARGE);

    // ---- Font sizes ----
    public static final String TITLE_SIZE = "22px";
    public static final String SUBTITLE_SIZE = "20px";
    public static final String BODY_SIZE = "14px";
    public static final String SMALL_SIZE = "12px";

    public static String screenBackground() {
        return "-fx-background-color: " + BACKGROUND + ";";
    }

    public static String cardStyle() {
        return "-fx-background-color: " + CARD_BACKGROUND + "; "
                + "-fx-background-radius: 10; -fx-border-radius: 10; "
                + "-fx-border-color: " + BORDER + "; -fx-border-width: 1;";
    }

    public static Label title(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: " + TITLE_SIZE + "; -fx-font-weight: bold; "
                + "-fx-text-fill: " + PRIMARY + ";");
        return label;
    }

    public static Label subtitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: " + SUBTITLE_SIZE + "; -fx-font-weight: bold; "
                + "-fx-text-fill: " + PRIMARY + ";");
        return label;
    }

    public static Label muted(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: " + SMALL_SIZE + "; -fx-text-fill: " + MUTED_TEXT + ";");
        return label;
    }

    public static Button primaryButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8;");
        return button;
    }

    public static Button successButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + SUCCESS + "; -fx-text-fill: white; "
                + "-fx-background-radius: 6;");
        return button;
    }

    public static Button dangerButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + DANGER + "; -fx-text-fill: white; "
                + "-fx-background-radius: 6;");
        return button;
    }

    public static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: " + PRIMARY + "; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; "
                + "-fx-border-color: " + PRIMARY + "; -fx-border-radius: 8;");
        return button;
    }

    /** A flexible spacer for pushing HBox/VBox content apart. */
    public static Region spacer() {
        Region region = new Region();
        return region;
    }
}
