package dev.rebelcraft.html.catalog;

import dev.rebelcraft.html.preview.Preview;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class CatalogRunnerTest {

    @TempDir
    File tempDir;

    // Fixture used to guarantee at least one @Preview is available when scanning test-classes
    public static class RunnerFixture {
        @Preview(value = "Runner Test Preview", group = "RunnerGroup")
        public String preview() {
            return "<p>runner</p>";
        }
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void outputDirArgControlsDestination() throws Exception {
        String outPath = new File(tempDir, "my-catalog").getAbsolutePath();
        CatalogRunner.run(new String[]{ "--output-dir", outPath });

        File index = new File(outPath, "index.html");
        assertTrue(index.exists(), "index.html should be created in the specified output dir");
    }

    @Test
    void generatesValidHtmlFile() throws Exception {
        String outPath = new File(tempDir, "catalog").getAbsolutePath();
        CatalogRunner.run(new String[]{ "--output-dir", outPath });

        String content = Files.readString(new File(outPath, "index.html").toPath());
        assertTrue(content.contains("<!DOCTYPE html>"), "Output should be a valid HTML document");
        assertTrue(content.contains("HTML Catalog"), "Output should contain the catalog title");
    }

    @Test
    void unknownArgsAreIgnored() throws Exception {
        String outPath = new File(tempDir, "safe").getAbsolutePath();
        // Should not throw
        assertDoesNotThrow(() ->
                CatalogRunner.run(new String[]{ "--unknown-flag", "value", "--output-dir", outPath }));
    }
}
