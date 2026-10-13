package io.github.teggr.examples.jte;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import io.github.teggr.html.preview.PreviewRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GreetingApplicationTest {

    @Test
    @ResourceLock(Resources.SYSTEM_OUT)
    void previewRunnerRendersExampleTemplate() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            PreviewRunner.main(new String[]{GreetingPreviews.class.getName(), "welcome"});
        } finally {
            System.setOut(originalOut);
        }
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("<h1>Hello, Ada!</h1>"));
    }

    @Test
    void servesJtePageOverHttp() throws Exception {
        var server = GreetingApplication.createServer(0);
        server.start();
        try {
            URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + "/");
            HttpClient client = HttpClient.newHttpClient();
            var response = client.send(HttpRequest.newBuilder(uri).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("text/html; charset=UTF-8",
                    response.headers().firstValue("Content-Type").orElseThrow());
            assertTrue(response.body().contains("<h1>Hello, Ada!</h1>"));
            assertEquals(404, client.send(HttpRequest.newBuilder(uri.resolve("/missing")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).statusCode());
        } finally {
            server.stop(0);
        }
    }
}
