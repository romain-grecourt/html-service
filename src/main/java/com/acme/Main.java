package com.acme;

import io.helidon.common.LogConfig;
import io.helidon.common.reactive.Single;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

/**
 * The application main class.
 */
public final class Main {

    /**
     * Cannot be instantiated.
     */
    private Main() {
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        startServer();
    }

    /**
     * Start the server.
     *
     * @return the created {@link WebServer} instance
     */
    static Single<WebServer> startServer() {

        // load logging configuration
        LogConfig.configureRuntime();

        WebServer server = WebServer.builder()
                                    .port(8080)
                                    .addRouting(Routing.builder()
                                                       .register("/html", new HTMLService())
                                                       .register("/test", new TestService()))
                                    .build();

        Single<WebServer> webserver = server.start();

        // Try to start the server. If successful, print some info and arrange to
        // print a message at shutdown. If unsuccessful, print the exception.
        webserver.thenAccept(ws -> {
                     System.out.println("WEB server is up! http://localhost:" + ws.port() + "/greet");
                     ws.whenShutdown().thenRun(() -> System.out.println("WEB server is DOWN. Good bye!"));
                 })
                 .exceptionallyAccept(t -> {
                     System.err.println("Startup failed: " + t.getMessage());
                     t.printStackTrace(System.err);
                 });

        return webserver;
    }
}
