package io.github.teggr.examples.jte;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.spi.PreviewGenerators;

public class GreetingApplication {

    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        HttpServer server = createServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        server.start();
        System.out.println("JTE example: http://localhost:" + server.getAddress().getPort());
    }

    static HttpServer createServer(int port) throws Exception {
        var method = GreetingPreviews.class.getMethod("welcome");
        byte[] html = PreviewGenerators.generate(GreetingPreviews.class, method,
                method.getAnnotation(Preview.class)).getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/", exchange -> {
            try {
                if (!exchange.getRequestURI().getPath().equals("/")) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, html.length);
                exchange.getResponseBody().write(html);
            } finally {
                exchange.close();
            }
        });
        return server;
    }
}
