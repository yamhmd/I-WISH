package com.iwish.iwishclient;

import com.iwish.iwishclient.network.SocketApiClient;
import com.iwish.iwishclient.session.Session;
import com.iwish.iwishclient.ui.AuthScreen;
import com.iwish.iwishclient.ui.MainScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) {
        Session.get().setApi(new SocketApiClient("localhost", 5000));

        showAuthScreen(stage);
        stage.setTitle("i-Wish");
        stage.show();
    }

    private void showAuthScreen(@org.jetbrains.annotations.NotNull Stage stage) {
        AuthScreen authScreen = new AuthScreen(() ->
                stage.setScene(new Scene(new MainScreen(() -> {
                    Session.get().logout();
                    showAuthScreen(stage);
                }), 900, 620))
        );
        stage.setScene(new Scene(authScreen, 420, 520));
    }

    public static void main(String[] args) {
        launch();
    }
}
