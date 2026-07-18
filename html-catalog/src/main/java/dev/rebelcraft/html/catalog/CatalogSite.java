package dev.rebelcraft.html.catalog;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates a self-contained static HTML catalog from a list of
 * {@link RenderedEntry} objects.
 *
 * <p>Output layout inside the chosen output directory:
 * <pre>
 *   index.html                             — two-panel shell (sidebar + iframe area)
 *   frames/{class-path}/{methodName}.html  — isolated per-preview page loaded in the iframe
 * </pre>
 *
 * <p>No external CSS/JS dependencies are required; all assets are embedded inline.
 */
public class CatalogSite {

    /**
     * Generates the catalog into {@code outputDir}, creating it if necessary.
     *
     * @param entries   the rendered preview entries to include
     * @param outputDir the root directory for the generated site
     * @throws IOException if writing any file fails
     */
    public void generate(List<RenderedEntry> entries, File outputDir) throws IOException {
        outputDir.mkdirs();

        File framesDir = new File(outputDir, "frames");
        framesDir.mkdirs();

        for (RenderedEntry entry : entries) {
            generateFrame(entry, framesDir);
        }

        generateIndex(entries, outputDir);
    }

    // -------------------------------------------------------------------------
    // Frame pages
    // -------------------------------------------------------------------------

    /**
     * Writes an isolated HTML page for a single preview entry so that it can
     * be loaded inside an {@code <iframe>} without style bleed from the shell.
     */
    private void generateFrame(RenderedEntry entry, File framesDir) throws IOException {
        String classPath = classToPath(entry.entry().className());
        File classDir = new File(framesDir, classPath);
        classDir.mkdirs();

        File frameFile = new File(classDir, entry.entry().methodName() + ".html");

        String body;
        if (entry.hasError()) {
            body = "<p style=\"color:#c0392b;font-family:sans-serif\"><strong>Render error:</strong> "
                    + escapeHtml(entry.error()) + "</p>";
        } else {
            body = entry.html() != null ? entry.html() : "";
        }

        String page = "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "</head>\n"
                + "<body style=\"margin:16px\">\n"
                + body + "\n"
                + "</body>\n"
                + "</html>\n";

        Files.writeString(frameFile.toPath(), page, StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Index page
    // -------------------------------------------------------------------------

    private void generateIndex(List<RenderedEntry> entries, File outputDir) throws IOException {
        String catalogJson = buildCatalogJson(entries);
        String html = INDEX_TEMPLATE.replace("/*CATALOG_JSON*/", catalogJson);
        Files.writeString(new File(outputDir, "index.html").toPath(), html, StandardCharsets.UTF_8);
    }

    /**
     * Builds the JSON object that is embedded in {@code index.html} as the
     * {@code catalog} JavaScript constant.
     */
    private String buildCatalogJson(List<RenderedEntry> entries) {
        // Group entries preserving insertion order (scanner already sorts them)
        Map<String, List<RenderedEntry>> byGroup = new LinkedHashMap<>();
        for (RenderedEntry entry : entries) {
            byGroup.computeIfAbsent(entry.entry().group(), k -> new java.util.ArrayList<>()).add(entry);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"groups\":[");

        boolean firstGroup = true;
        for (Map.Entry<String, List<RenderedEntry>> groupEntry : byGroup.entrySet()) {
            if (!firstGroup) sb.append(',');
            firstGroup = false;

            sb.append("{\"name\":").append(jsonString(groupEntry.getKey()));
            sb.append(",\"items\":[");

            boolean firstItem = true;
            for (RenderedEntry re : groupEntry.getValue()) {
                if (!firstItem) sb.append(',');
                firstItem = false;

                String frameSrc = "frames/" + classToPath(re.entry().className())
                        + "/" + re.entry().methodName() + ".html";

                sb.append("{\"name\":").append(jsonString(re.entry().name()));
                sb.append(",\"description\":").append(jsonString(re.entry().description()));
                sb.append(",\"frameSrc\":").append(jsonString(frameSrc));
                sb.append("}");
            }

            sb.append("]}");
        }

        sb.append("]}");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /**
     * Converts a fully-qualified class name to a safe relative file-system path
     * segment by replacing {@code .} with {@code /} and {@code $} with {@code _}.
     *
     * <p>Example: {@code com.example.Outer$Inner} → {@code com/example/Outer_Inner}
     */
    static String classToPath(String className) {
        return className.replace('.', '/').replace('$', '_');
    }

    /** Escapes a Java string value for safe embedding in a JSON string literal. */
    static String jsonString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 4);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** Escapes HTML special characters for safe embedding in HTML attribute values and text. */
    static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // -------------------------------------------------------------------------
    // Embedded HTML template
    // -------------------------------------------------------------------------

    /**
     * Self-contained HTML shell for the catalog browser.
     * The {@code CATALOG_JSON} token (surrounded by a JS block-comment) is
     * replaced at generation time with the actual JSON data object.
     */
    private static final String INDEX_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>HTML Component Catalog</title>
            <style>
            *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
            html, body { height: 100%; }
            body { display: flex; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; font-size: 14px; background: #13131a; color: #e0e0e8; overflow: hidden; }
            #sidebar { width: 240px; min-width: 200px; background: #1a1a27; border-right: 1px solid #2a2a3e; display: flex; flex-direction: column; overflow-y: auto; flex-shrink: 0; }
            #sidebar-header { padding: 16px 14px 12px; font-size: 13px; font-weight: 700; letter-spacing: 0.05em; text-transform: uppercase; color: #7c88ff; border-bottom: 1px solid #2a2a3e; }
            .group { margin-bottom: 4px; }
            .group-name { padding: 8px 14px 4px; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.1em; color: #6a6a8e; }
            .item { display: block; padding: 6px 14px 6px 22px; font-size: 13px; color: #b0b0c8; cursor: pointer; border: none; background: none; width: 100%; text-align: left; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
            .item:hover { background: #242436; color: #e0e0f0; }
            .item.active { background: #2c2c44; color: #9090ff; font-weight: 500; }
            #main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
            #toolbar { padding: 10px 16px; background: #1a1a27; border-bottom: 1px solid #2a2a3e; display: flex; align-items: flex-start; gap: 8px; min-height: 48px; flex-shrink: 0; }
            #title-area { flex: 1; }
            #preview-name { font-size: 15px; font-weight: 600; color: #e0e0f0; line-height: 1.4; }
            #preview-desc { font-size: 12px; color: #6a6a8e; margin-top: 2px; }
            .tab-group { display: flex; gap: 4px; align-items: center; padding-top: 2px; }
            .tab { padding: 4px 10px; font-size: 12px; cursor: pointer; border-radius: 4px; border: 1px solid #2a2a3e; background: transparent; color: #8888aa; font-family: inherit; }
            .tab:hover { background: #242436; }
            .tab.active { background: #2c2c44; color: #9090ff; border-color: #4040aa; }
            #content { flex: 1; position: relative; overflow: hidden; }
            #empty-state { display: flex; align-items: center; justify-content: center; height: 100%; color: #444460; font-size: 14px; }
            #preview-frame { display: none; width: 100%; height: 100%; border: none; background: white; }
            #source-panel { display: none; width: 100%; height: 100%; overflow: auto; padding: 20px; background: #13131a; }
            #source-panel pre { font-family: "Cascadia Code", "Fira Code", Consolas, monospace; font-size: 13px; color: #a0ffa0; white-space: pre-wrap; word-break: break-word; line-height: 1.6; }
            </style>
            </head>
            <body>
            <nav id="sidebar">
              <div id="sidebar-header">HTML Catalog</div>
              <div id="sidebar-groups"></div>
            </nav>
            <div id="main">
              <div id="toolbar">
                <div id="title-area">
                  <div id="preview-name">Select a component</div>
                  <div id="preview-desc"></div>
                </div>
                <div class="tab-group">
                  <button class="tab active" id="tab-preview" onclick="showTab('preview')">Preview</button>
                  <button class="tab" id="tab-source" onclick="showTab('source')">Source</button>
                </div>
              </div>
              <div id="content">
                <div id="empty-state">&#8592; Select a component from the sidebar</div>
                <iframe id="preview-frame" title="Component Preview"></iframe>
                <div id="source-panel"><pre id="source-code"></pre></div>
              </div>
            </div>
            <script>
            var catalog = /*CATALOG_JSON*/;
            var currentItem = null;
            var activeTab = 'preview';
            function renderSidebar() {
              var container = document.getElementById('sidebar-groups');
              if (!catalog.groups || catalog.groups.length === 0) {
                container.innerHTML = '<div style="padding:16px;color:#444460;font-size:12px;">No @Preview methods found.</div>';
                return;
              }
              catalog.groups.forEach(function(group) {
                var groupEl = document.createElement('div');
                groupEl.className = 'group';
                var nameEl = document.createElement('div');
                nameEl.className = 'group-name';
                nameEl.textContent = group.name;
                groupEl.appendChild(nameEl);
                group.items.forEach(function(item) {
                  var btn = document.createElement('button');
                  btn.className = 'item';
                  btn.textContent = item.name;
                  btn.title = item.description || '';
                  btn.addEventListener('click', function() { selectItem(item, btn); });
                  groupEl.appendChild(btn);
                });
                container.appendChild(groupEl);
              });
            }
            function selectItem(item, btn) {
              document.querySelectorAll('.item').forEach(function(el) { el.classList.remove('active'); });
              btn.classList.add('active');
              document.getElementById('preview-name').textContent = item.name;
              document.getElementById('preview-desc').textContent = item.description || '';
              currentItem = item;
              showTab(activeTab);
            }
            function showTab(tab) {
              activeTab = tab;
              document.getElementById('empty-state').style.display = 'none';
              var frame = document.getElementById('preview-frame');
              var srcPanel = document.getElementById('source-panel');
              document.getElementById('tab-preview').classList.toggle('active', tab === 'preview');
              document.getElementById('tab-source').classList.toggle('active', tab === 'source');
              if (tab === 'preview') {
                if (currentItem) { frame.src = currentItem.frameSrc; frame.style.display = 'block'; }
                srcPanel.style.display = 'none';
              } else {
                frame.style.display = 'none';
                srcPanel.style.display = 'block';
                var pre = document.getElementById('source-code');
                pre.textContent = '// Source not available in this version.';
              }
            }
            renderSidebar();
            </script>
            </body>
            </html>
            """;
}
