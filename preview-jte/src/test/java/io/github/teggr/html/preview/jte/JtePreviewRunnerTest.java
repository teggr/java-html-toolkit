package io.github.teggr.html.preview.jte;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

import gg.jte.TemplateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.PreviewRunner;
import io.github.teggr.html.preview.spi.PreviewGenerator;

import static org.junit.jupiter.api.Assertions.*;

public class JtePreviewRunnerTest {

    public record Person(String name) {
    }

    public static class GreetingPreview {
        @Preview("Greeting")
        public JtePreview greeting() {
            return new JtePreview("pages/greeting.jte",
                    Map.of("person", new Person("Ada <&>")));
        }

        @Preview
        public String plain() {
            return "<p>String preview</p>";
        }

        @Preview
        public JtePreview missing() {
            return new JtePreview("missing.jte", Map.of());
        }

        @Preview
        public JtePreview empty() {
            return null;
        }

        public JtePreview withArgument(String name) {
            return new JtePreview("pages/greeting.jte", Map.of());
        }
    }

    @Test
    @ResourceLock(Resources.SYSTEM_OUT)
    void rendersClasspathTemplateThroughPreviewRunnerAndKeepsStringPreviews() {
        assertEquals("<main>Hello, Ada &lt;&amp;&gt;!</main>", run("greeting"));
        assertEquals("<p>String preview</p>", run("plain"));
        assertEquals("<main>Hello, Ada &lt;&amp;&gt;!</main>", run("greeting"));
    }

    private String run(String method) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            PreviewRunner.main(new String[]{GreetingPreview.class.getName(), method});
        } finally {
            System.setOut(originalOut);
        }
        return output.toString(StandardCharsets.UTF_8).stripTrailing();
    }

    @Test
    void registersAdapterAndOnlySupportsItsNoArgumentContract() throws Exception {
        PreviewGenerator generator = ServiceLoader.load(PreviewGenerator.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> provider.id().equals("jte")).findFirst().orElseThrow();
        assertTrue(generator.supports(GreetingPreview.class,
                GreetingPreview.class.getMethod("greeting")));
        assertFalse(generator.supports(GreetingPreview.class,
                GreetingPreview.class.getMethod("plain")));
        assertFalse(generator.supports(GreetingPreview.class,
                GreetingPreview.class.getMethod("withArgument", String.class)));
    }

    @Test
    void reportsNullResultsAndMissingTemplates() throws Exception {
        var empty = GreetingPreview.class.getMethod("empty");
        assertThrows(IllegalStateException.class, () -> new JtePreviewGenerator()
                .generate(GreetingPreview.class, empty, empty.getAnnotation(Preview.class)));
        var missing = GreetingPreview.class.getMethod("missing");
        assertThrows(TemplateException.class, () -> new JtePreviewGenerator()
                .generate(GreetingPreview.class, missing, missing.getAnnotation(Preview.class)));
    }

    @Test
    void validatesContractAndCopiesParametersIncludingNullValues() {
        assertThrows(NullPointerException.class, () -> new JtePreview(null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new JtePreview(" ", Map.of()));
        assertThrows(NullPointerException.class, () -> new JtePreview("greeting.jte", null));
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", null);
        JtePreview preview = new JtePreview("greeting.jte", params);
        params.put("name", "changed");
        assertTrue(preview.params().containsKey("name"));
        assertNull(preview.params().get("name"));
        assertThrows(UnsupportedOperationException.class,
                () -> preview.params().put("name", "changed"));
    }
}
