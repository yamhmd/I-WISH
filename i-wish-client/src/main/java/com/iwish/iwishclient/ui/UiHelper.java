package com.iwish.iwishclient.ui;

import com.iwish.iwishclient.network.ApiException;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.function.Consumer;

public class UiHelper {

    @FunctionalInterface
    public interface ApiCall<T> {
        T call() throws ApiException;
    }

    @FunctionalInterface
    public interface ApiAction {
        void run() throws ApiException;
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /** For calls that return a value (login, viewFriends, ...) */
    public static <T> void runAsync(ApiCall<T> apiCall, Consumer<T> onSuccess, Node... disableWhileLoading) {
        setDisabled(true, disableWhileLoading);

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return apiCall.call();
            }
        };

        task.setOnSucceeded(e -> {
            setDisabled(false, disableWhileLoading);
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(e -> {
            setDisabled(false, disableWhileLoading);
            Throwable ex = task.getException();
            if (ex instanceof ApiException) {
                showError(ex.getMessage());
            } else {
                showError("Could not reach the server. Please try again.");
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /** For calls with no return value (addFriend, acceptFriend, ...) */
    public static void runVoid(ApiAction action, Runnable onSuccess, Node... disableWhileLoading) {
        runAsync(() -> {
            action.run();
            return null;
        }, v -> onSuccess.run(), disableWhileLoading);
    }

    private static void setDisabled(boolean disabled, Node... nodes) {
        for (Node n : nodes) {
            n.setDisable(disabled);
        }
    }
}
