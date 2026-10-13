package io.github.teggr.html.catalog;

import io.github.teggr.html.preview.Preview;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CatalogScannerTest {

    // -------------------------------------------------------------------------
    // Fixtures — annotated inner classes discovered by the scanner
    // -------------------------------------------------------------------------

    public static class ButtonPreviews {
        @Preview(value = "Primary Button", group = "Buttons", description = "A primary action button", order = 1)
        public String primary() {
            return "<button class=\"btn-primary\">Click</button>";
        }

        @Preview(value = "Secondary Button", group = "Buttons", order = 2)
        public String secondary() {
            return "<button class=\"btn-secondary\">Cancel</button>";
        }
    }

    public static class FormPreviews {
        @Preview(value = "Text Input", group = "Forms")
        public String textInput() {
            return "<input type=\"text\" placeholder=\"Enter text\">";
        }
    }

    public static class DefaultGroupPreviews {
        @Preview("Unlabelled")
        public String unlabelled() {
            return "<p>hello</p>";
        }
    }

    // A class with NO @Preview methods — should not produce any entries
    public static class NonPreview {
        public String notAnnotated() {
            return "ignored";
        }
    }

    public static class MalformedPreviews {
        @Preview(value = "Argument Preview", group = "Malformed")
        public String withArgument(String value) {
            return value;
        }

        @Preview(value = "Void Preview", group = "Malformed")
        public void noReturnValue() {
        }
    }

    public static class OverloadedPreviews {
        @Preview(value = "Overloaded", group = "Overloads")
        public String overloaded() {
            return "<p>no arguments</p>";
        }

        @Preview(value = "Overloaded", group = "Overloads")
        public String overloaded(String value) {
            return value;
        }
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void findsAnnotatedMethodsInDirectory() throws Exception {
        CatalogScanner scanner = new CatalogScanner();
        List<CatalogEntry> all = scanTestClasses(scanner);

        // Must find both ButtonPreviews methods
        long buttonCount = all.stream()
                .filter(e -> e.className().equals(ButtonPreviews.class.getName()))
                .count();
        assertEquals(2, buttonCount, "Expected 2 entries for ButtonPreviews");
    }

    @Test
    void respectsGroupAttribute() throws Exception {
        List<CatalogEntry> all = scanTestClasses(new CatalogScanner());

        CatalogEntry primary = all.stream()
                .filter(e -> e.className().equals(ButtonPreviews.class.getName()) && "primary".equals(e.methodName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("primary entry not found"));

        assertEquals("Buttons", primary.group());
        assertEquals("Primary Button", primary.name());
        assertEquals("A primary action button", primary.description());
        assertEquals(1, primary.order());
    }

    @Test
    void defaultsGroupToSimpleClassName() throws Exception {
        List<CatalogEntry> all = scanTestClasses(new CatalogScanner());

        CatalogEntry entry = all.stream()
                .filter(e -> e.className().equals(DefaultGroupPreviews.class.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("DefaultGroupPreviews entry not found"));

        assertEquals("DefaultGroupPreviews", entry.group());
        assertEquals("Unlabelled", entry.name());
    }

    @Test
    void defaultsNameToMethodName() throws Exception {
        // @Preview with no value() defaults to method name
        List<CatalogEntry> all = scanTestClasses(new CatalogScanner());

        CatalogEntry entry = all.stream()
                .filter(e -> e.className().equals(DefaultGroupPreviews.class.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("DefaultGroupPreviews entry not found"));

        // value="Unlabelled" was explicitly set, so name should be that
        assertEquals("Unlabelled", entry.name());
    }

    @Test
    void skipsClassesWithoutAnnotation() throws Exception {
        List<CatalogEntry> all = scanTestClasses(new CatalogScanner());

        boolean nonPreviewFound = all.stream()
                .anyMatch(e -> e.className().equals(NonPreview.class.getName()));
        assertFalse(nonPreviewFound, "NonPreview class should not appear in scan results");
    }

    @Test
    void sortsByGroupThenOrderThenName() throws Exception {
        List<CatalogEntry> all = scanTestClasses(new CatalogScanner());

        // Filter to just ButtonPreviews entries
        List<CatalogEntry> buttons = all.stream()
                .filter(e -> "Buttons".equals(e.group()))
                .toList();

        assertEquals(2, buttons.size());
        assertEquals("Primary Button", buttons.get(0).name());
        assertEquals("Secondary Button", buttons.get(1).name());
    }

    @Test
    void keepsUnsupportedAndOverloadedMethodsDistinct() throws Exception {
        List<CatalogEntry> all = scanTestClasses(new CatalogScanner());

        List<CatalogEntry> malformed = all.stream()
                .filter(entry -> entry.className().equals(MalformedPreviews.class.getName()))
                .toList();
        assertEquals(2, malformed.size());

        List<CatalogEntry> overloaded = all.stream()
                .filter(entry -> entry.className().equals(OverloadedPreviews.class.getName()))
                .toList();
        assertEquals(2, overloaded.size());
        assertEquals(2, overloaded.stream().map(CatalogEntry::methodSignature).distinct().count());
    }

    @Test
    void deduplicatesRepeatedClassPathRoots() throws Exception {
        URL location = CatalogScannerTest.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[]{location, location},
                Thread.currentThread().getContextClassLoader()
        )) {
            List<CatalogEntry> entries = new CatalogScanner().scan(classLoader);
            List<CatalogEntry> buttonEntries = entries.stream()
                    .filter(entry -> entry.className().equals(ButtonPreviews.class.getName()))
                    .toList();

            Set<String> signatures = new HashSet<>();
            buttonEntries.forEach(entry -> signatures.add(entry.methodSignature()));
            assertEquals(signatures.size(), buttonEntries.size());
            assertEquals(2, buttonEntries.size());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Scans the test-classes directory so that only this module's own compiled
     * test classes are visible (not any JARs).
     */
    private List<CatalogEntry> scanTestClasses(CatalogScanner scanner) throws Exception {
        URL location = CatalogScannerTest.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader cl = new URLClassLoader(
                new URL[]{ location },
                Thread.currentThread().getContextClassLoader()
        )) {
            return scanner.scan(cl);
        }
    }
}
