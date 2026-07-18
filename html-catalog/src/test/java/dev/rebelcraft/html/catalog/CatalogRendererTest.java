package dev.rebelcraft.html.catalog;

import dev.rebelcraft.html.preview.Preview;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogRendererTest {

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    public static class StringPreviews {
        @Preview(value = "Hello", group = "Basic")
        public String hello() {
            return "<p>Hello, catalog!</p>";
        }

        @Preview(value = "Null Result", group = "Basic")
        public String nullResult() {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void rendersStringReturningMethod() {
        CatalogEntry entry = new CatalogEntry(
                "Basic", "Hello", "", 0,
                StringPreviews.class.getName(), "hello"
        );

        CatalogRenderer renderer = new CatalogRenderer();
        List<RenderedEntry> results = renderer.render(List.of(entry));

        assertEquals(1, results.size());
        RenderedEntry result = results.get(0);
        assertFalse(result.hasError(), "Should not have an error");
        assertEquals("<p>Hello, catalog!</p>", result.html());
        assertSame(entry, result.entry());
    }

    @Test
    void rendersNullResultAsEmptyString() {
        CatalogEntry entry = new CatalogEntry(
                "Basic", "Null Result", "", 0,
                StringPreviews.class.getName(), "nullResult"
        );

        CatalogRenderer renderer = new CatalogRenderer();
        List<RenderedEntry> results = renderer.render(List.of(entry));

        RenderedEntry result = results.get(0);
        assertFalse(result.hasError());
        assertEquals("", result.html());
    }

    @Test
    void capturesErrorForUnresolvableClass() {
        CatalogEntry entry = new CatalogEntry(
                "Bad", "Missing", "", 0,
                "com.example.DoesNotExist", "preview"
        );

        CatalogRenderer renderer = new CatalogRenderer();
        List<RenderedEntry> results = renderer.render(List.of(entry));

        RenderedEntry result = results.get(0);
        assertTrue(result.hasError(), "Should capture the class-not-found error");
        assertNull(result.html());
    }

    @Test
    void capturesErrorForMissingMethod() {
        CatalogEntry entry = new CatalogEntry(
                "Bad", "Wrong method", "", 0,
                StringPreviews.class.getName(), "noSuchMethod"
        );

        CatalogRenderer renderer = new CatalogRenderer();
        List<RenderedEntry> results = renderer.render(List.of(entry));

        RenderedEntry result = results.get(0);
        assertTrue(result.hasError(), "Should capture the no-such-method error");
    }

    @Test
    void preservesOrderOfEntries() {
        CatalogEntry e1 = new CatalogEntry("G", "First", "", 0, StringPreviews.class.getName(), "hello");
        CatalogEntry e2 = new CatalogEntry("G", "Second", "", 1, StringPreviews.class.getName(), "nullResult");

        CatalogRenderer renderer = new CatalogRenderer();
        List<RenderedEntry> results = renderer.render(List.of(e1, e2));

        assertEquals(2, results.size());
        assertSame(e1, results.get(0).entry());
        assertSame(e2, results.get(1).entry());
    }
}
