package com.iwish.iwishclient;

import com.iwish.iwishclient.network.MockApiClient;
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

        AuthScreen authScreen = new AuthScreen(() ->
                stage.setScene(new Scene(new MainScreen(), 700, 500))
        );

        stage.setTitle("i-Wish");
        stage.setScene(new Scene(authScreen, 420, 520));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
