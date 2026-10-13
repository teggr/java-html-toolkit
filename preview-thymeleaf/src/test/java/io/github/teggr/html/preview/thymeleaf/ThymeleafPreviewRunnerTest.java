package io.github.teggr.html.preview.thymeleaf;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.PreviewRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThymeleafPreviewRunnerTest {

    public static class GreetingPreview {
        @Preview("Greeting")
        public ThymeleafPreview greeting() {
            return new ThymeleafPreview("greeting", Map.of("name", "Ada"));
        }
    }

    @Test
    @ResourceLock(Resources.SYSTEM_OUT)
    void rendersClasspathTemplateThroughPreviewRunner() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            PreviewRunner.main(new String[]{
                    GreetingPreview.class.getName(), "greeting"
            });
        } finally {
            System.setOut(originalOut);
        }

        assertEquals("<main>Hello, <span>Ada</span>!</main>",
                output.toString(StandardCharsets.UTF_8).stripTrailing());
    }
}
