package dev.rebelcraft.html.catalog;

import java.io.File;
import java.util.List;

/**
 * Command-line entry point for the HTML component catalog generator.
 *
 * <p>Scans the current JVM classpath for {@code @Preview}-annotated methods,
 * renders each one, and writes a self-contained static HTML catalog to the
 * output directory.
 *
 * <p>Usage:
 * <pre>
 *   java -cp &lt;project-classpath&gt; dev.rebelcraft.html.catalog.CatalogRunner [--output-dir &lt;path&gt;]
 * </pre>
 *
 * <p>When invoked via the Maven exec plugin the classpath is set up automatically:
 * <pre>{@code
 * <plugin>
 *   <groupId>org.codehaus.mojo</groupId>
 *   <artifactId>exec-maven-plugin</artifactId>
 *   <executions>
 *     <execution>
 *       <id>generate-catalog</id>
 *       <phase>verify</phase>
 *       <goals><goal>java</goal></goals>
 *       <configuration>
 *         <mainClass>dev.rebelcraft.html.catalog.CatalogRunner</mainClass>
 *       </configuration>
 *     </execution>
 *   </executions>
 * </plugin>
 * }</pre>
 */
public class CatalogRunner {

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.err.println("[html-catalog] Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Runs the catalog generation pipeline. Separated from {@link #main} so that
     * tests can invoke it without triggering {@link System#exit}.
     *
     * @param args command-line arguments (see class javadoc)
     * @throws Exception if site generation fails
     */
    static void run(String[] args) throws Exception {
        File outputDir = new File("target/html-catalog");

        for (int i = 0; i < args.length; i++) {
            if ("--output-dir".equals(args[i]) && i + 1 < args.length) {
                outputDir = new File(args[++i]);
            }
        }

        System.out.println("[html-catalog] Scanning classpath for @Preview methods...");

        CatalogScanner scanner = new CatalogScanner();
        List<CatalogEntry> entries = scanner.scan();

        System.out.println("[html-catalog] Found " + entries.size() + " preview(s)");

        CatalogRenderer renderer = new CatalogRenderer();
        List<RenderedEntry> rendered = renderer.render(entries);

        long errors = rendered.stream().filter(RenderedEntry::hasError).count();
        if (errors > 0) {
            System.out.println("[html-catalog] Warning: " + errors + " preview(s) failed to render");
        }

        CatalogSite site = new CatalogSite();
        site.generate(rendered, outputDir);

        System.out.println("[html-catalog] Catalog generated: "
                + outputDir.getAbsolutePath() + File.separator + "index.html");
    }
}
