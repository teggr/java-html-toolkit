package dev.rebelcraft.html.preview;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a no-argument method as an HTML preview.
 *
 * <p>The annotated method must be public, take no arguments, and return either a
 * {@link String} of HTML or an object exposing a {@code render()} method.
 * The Java HTML Tooling VS Code extension discovers
 * methods carrying this annotation, compiles the surrounding Maven project, and
 * renders the returned HTML inside an editor side-panel.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class MyComponentPreview {
 *
 *     @Preview("Main layout")
 *     public String mainLayout() {
 *         return "<h1>Hello, HTML tooling!</h1>";
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Preview {

    /**
     * An optional human-readable name shown in the preview panel title.
     * Defaults to the method name when left blank.
     */
    String value() default "";
}