package dev.rebelcraft.html.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogSiteTest {

    @TempDir
    File tempDir;

    // -------------------------------------------------------------------------
    // Helper builders
    // -------------------------------------------------------------------------

    private CatalogEntry entry(String group, String name, String className, String method) {
        return new CatalogEntry(group, name, "", 0, className, method);
    }

    private RenderedEntry ok(CatalogEntry e, String html) {
        return new RenderedEntry(e, html, null);
    }

    private RenderedEntry error(CatalogEntry e, String msg) {
        return new RenderedEntry(e, null, msg);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void generatesIndexHtml() throws Exception {
        CatalogEntry e = entry("Buttons", "Primary Button", "com.example.ButtonPreviews", "primary");
        new CatalogSite().generate(List.of(ok(e, "<button>Click</button>")), tempDir);

        File index = new File(tempDir, "index.html");
        assertTrue(index.exists(), "index.html should be created");

        String html = Files.readString(index.toPath());
        assertTrue(html.contains("<!DOCTYPE html>"), "Should be a valid HTML document");
        assertTrue(html.contains("HTML Catalog"), "Should include catalog title");
        assertTrue(html.contains("Primary Button"), "Should include entry name");
        assertTrue(html.contains("Buttons"), "Should include group name");
    }

    @Test
    void generatesFramePage() throws Exception {
        CatalogEntry e = entry("Forms", "Text Input", "com.example.FormPreviews", "textInput");
        new CatalogSite().generate(List.of(ok(e, "<input type=\"text\">")), tempDir);

        File frame = new File(tempDir, "frames/com/example/FormPreviews/textInput.html");
        assertTrue(frame.exists(), "Frame page should be created");

        String html = Files.readString(frame.toPath());
        assertTrue(html.contains("<input type=\"text\">"), "Frame should contain rendered HTML");
    }

    @Test
    void framePageContainsErrorMessageOnRenderFailure() throws Exception {
        CatalogEntry e = entry("Bad", "Broken", "com.example.Broken", "broken");
        new CatalogSite().generate(List.of(error(e, "Something went wrong")), tempDir);

        File frame = new File(tempDir, "frames/com/example/Broken/broken.html");
        assertTrue(frame.exists());

        String html = Files.readString(frame.toPath());
        assertTrue(html.contains("Something went wrong"), "Frame should show the error message");
        assertTrue(html.contains("Render error"), "Frame should indicate a render error");
    }

    @Test
    void groupsEntriesInIndexJson() throws Exception {
        CatalogEntry b1 = entry("Buttons", "Primary", "com.example.B", "primary");
        CatalogEntry b2 = entry("Buttons", "Secondary", "com.example.B", "secondary");
        CatalogEntry f1 = entry("Forms", "Input", "com.example.F", "input");

        new CatalogSite().generate(
                List.of(ok(b1, "<button>P</button>"), ok(b2, "<button>S</button>"), ok(f1, "<input>")),
                tempDir
        );

        String index = Files.readString(new File(tempDir, "index.html").toPath());
        // Both group names should appear in the embedded JSON
        assertTrue(index.contains("\"name\":\"Buttons\""), "Buttons group should be in JSON");
        assertTrue(index.contains("\"name\":\"Forms\""), "Forms group should be in JSON");
    }

    @Test
    void innerClassNameUsesUnderscoreInPath() throws Exception {
        // $-in class name should produce _ in file path
        CatalogEntry e = entry("X", "Nested", "com.example.Outer$Inner", "preview");
        new CatalogSite().generate(List.of(ok(e, "<p>nested</p>")), tempDir);

        File frame = new File(tempDir, "frames/com/example/Outer_Inner/preview.html");
        assertTrue(frame.exists(), "Inner class $ should be converted to _ in path");
    }

    @Test
    void generatesSiteWithNoEntries() throws Exception {
        new CatalogSite().generate(List.of(), tempDir);

        File index = new File(tempDir, "index.html");
        assertTrue(index.exists(), "index.html should be created even with no entries");

        String html = Files.readString(index.toPath());
        assertTrue(html.contains("No @Preview methods found"), "Should show empty-state message");
    }

    // -------------------------------------------------------------------------
    // Unit tests for utility methods
    // -------------------------------------------------------------------------

    @Test
    void classToPathConvertsDotsAndDollar() {
        assertEquals("com/example/Outer_Inner", CatalogSite.classToPath("com.example.Outer$Inner"));
        assertEquals("dev/rebelcraft/Preview", CatalogSite.classToPath("dev.rebelcraft.Preview"));
    }

    @Test
    void jsonStringEscapesSpecialChars() {
        assertEquals("\"hello\"", CatalogSite.jsonString("hello"));
        assertEquals("\"say \\\"hi\\\"\"", CatalogSite.jsonString("say \"hi\""));
        assertEquals("\"a\\nb\"", CatalogSite.jsonString("a\nb"));
        assertEquals("null", CatalogSite.jsonString(null));
    }

    @Test
    void escapeHtmlEscapesAngleBrackets() {
        assertEquals("&lt;p&gt;", CatalogSite.escapeHtml("<p>"));
        assertEquals("a &amp; b", CatalogSite.escapeHtml("a & b"));
        assertEquals("", CatalogSite.escapeHtml(null));
    }
}
