package com.iwish.iwishclient;
import iwish.server.IWishServer;
public class Launcher {
    public static void main(String[] args) {
        Thread serverThread = new Thread(() -> {
            try {
                IWishServer.main(args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "iwish-server");
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        HelloApplication.main(args);
    }
}
