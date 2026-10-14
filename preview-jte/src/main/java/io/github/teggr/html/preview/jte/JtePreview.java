package io.github.teggr.html.preview.jte;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Template and parameters returned by a JTE-backed {@code @Preview} method.
 *
 * @param template the template path relative to {@code templates/}, including {@code .jte}
 * @param params   values keyed by the template's {@code @param} names
 */
public record JtePreview(String template, Map<String, Object> params) {

    public JtePreview {
        Objects.requireNonNull(template, "template must not be null");
        if (template.isBlank()) {
            throw new IllegalArgumentException("template must not be blank");
        }
        Objects.requireNonNull(params, "params must not be null");
        params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }
}
