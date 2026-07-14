package com.sks.server;

import org.noear.solon.Solon;
import org.noear.solon.core.route.Router;

public class SolonApp {
    public static void main(String[] args) {
        Solon.start(SolonApp.class, args);

        System.out.println("================================================");
        System.out.println("Solon API Server started on port: " + Solon.cfg().serverPort());
        System.out.println("API Key: " + (Solon.cfg().get("solon.apiKey", "").isEmpty() ? "NOT SET" : "Configured"));
        System.out.println("Registered routes:");
        Router router = Solon.app().router();
        System.out.println("Router: " + router);
        System.out.println("================================================");
    }
}