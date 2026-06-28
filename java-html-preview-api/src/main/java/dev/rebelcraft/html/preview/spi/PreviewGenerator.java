package dev.rebelcraft.html.preview.spi;

import java.lang.reflect.Method;

import dev.rebelcraft.html.preview.Preview;

/**
 * Strategy for generating preview HTML for an annotated method.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 */
public interface PreviewGenerator {

    /**
     * Stable identifier for diagnostics and logging.
     */
    String id();

    /**
     * Higher values take precedence when multiple generators support a method.
     */
    int priority();

    /**
     * Returns true if this generator can generate preview output for the method.
     */
    boolean supports(Class<?> clazz, Method method);

    /**
     * Generates HTML for the preview method.
     */
    String generate(Class<?> clazz, Method method, Preview preview) throws Exception;
}
