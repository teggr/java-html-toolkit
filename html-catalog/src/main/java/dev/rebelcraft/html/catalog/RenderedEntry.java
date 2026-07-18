package dev.rebelcraft.html.catalog;

/**
 * Pairs a {@link CatalogEntry} with its rendered HTML output (or an error message).
 *
 * @param entry The catalog metadata for this preview.
 * @param html  The rendered HTML string, or {@code null} if rendering failed.
 * @param error A human-readable error message when rendering failed, otherwise {@code null}.
 */
public record RenderedEntry(
        CatalogEntry entry,
        String html,
        String error
) {

    /** Returns {@code true} when rendering produced an error instead of HTML. */
    public boolean hasError() {
        return error != null;
    }
}
