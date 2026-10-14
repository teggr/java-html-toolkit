package io.github.teggr.examples.jte;

import java.util.Map;

import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.jte.JtePreview;

public class GreetingPreviews {

    @Preview(value = "Welcome page", group = "JTE")
    public JtePreview welcome() {
        return new JtePreview("welcome.jte", Map.of("name", "Ada"));
    }
}
