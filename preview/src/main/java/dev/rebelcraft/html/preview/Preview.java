package dev.rebelcraft.html.preview;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a no-argument method as an HTML preview.
 *
 * <p>The annotated method must take no arguments and return a
 * {@link String} of HTML.
 * The method may be public, protected, package-private, or private; the runner
 * will access it reflectively.
 * The Java HTML Tooling VS Code extension discovers
 * methods carrying this annotation, compiles the surrounding Maven project, and
 * renders the returned HTML inside an editor side-panel.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class MyComponentPreview {
 *
 *     @Preview(value = "Primary Button", group = "Buttons", description = "A primary action button")
 *     public String primaryButton() {
 *         return "<button class=\"btn-primary\">Click me</button>";
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Preview {

    /**
     * An optional human-readable name shown in the preview panel title and catalog.
     * Defaults to the method name when left blank.
     */
    String value() default "";

    /**
     * The catalog group (component family) this preview belongs to.
     * Previews in the same group are listed together in the sidebar.
     * Defaults to the declaring class's simple name when left blank.
     */
    String group() default "";

    /**
     * An optional prose description shown alongside the preview in the catalog.
     */
    String description() default "";

    /**
     * Sort order within the group. Lower values appear first.
     * Defaults to {@code 0}; ties are broken alphabetically by name.
     */
    int order() default 0;
}