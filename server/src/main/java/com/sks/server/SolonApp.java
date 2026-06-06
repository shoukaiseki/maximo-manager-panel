package com.sks.server;

import org.noear.solon.Solon;

public class SolonApp {
    public static void main(String[] args) {
        Solon.start(SolonApp.class, args);

        System.out.println("================================================");
        System.out.println("Solon API Server started on port: " + Solon.cfg().serverPort());
        System.out.println("API Key: " + (Solon.cfg().get("solon.apiKey", "").isEmpty() ? "NOT SET" : "Configured"));
        System.out.println("================================================");
    }
}