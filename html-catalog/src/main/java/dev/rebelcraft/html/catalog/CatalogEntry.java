package dev.rebelcraft.html.catalog;

/**
 * Describes a single {@code @Preview}-annotated method discovered on the classpath.
 *
 * @param group      The catalog group (component family); defaults to the declaring class simple name.
 * @param name       Human-readable display name; defaults to the method name.
 * @param description Optional prose description shown in the catalog.
 * @param order      Sort order within the group.
 * @param className  Fully-qualified class name of the declaring class.
 * @param methodName Name of the annotated method.
 */
public record CatalogEntry(
        String group,
        String name,
        String description,
        int order,
        String className,
        String methodName
) {}
