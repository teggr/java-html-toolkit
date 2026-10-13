package io.github.teggr.html.preview.thymeleaf;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Template and model data returned by a Thymeleaf-backed {@code @Preview} method.
 *
 * @param template the template name relative to {@code templates/}, without an extension
 * @param model    variables made available to the Thymeleaf template
 */
public record ThymeleafPreview(String template, Map<String, Object> model) {

    public ThymeleafPreview {
        Objects.requireNonNull(template, "template must not be null");
        if (template.isBlank()) {
            throw new IllegalArgumentException("template must not be blank");
        }
        Objects.requireNonNull(model, "model must not be null");
        model = Collections.unmodifiableMap(new LinkedHashMap<>(model));
    }
}
