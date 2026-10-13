# Java HTML Tooling – VS Code Extension

Adds an Xcode-style `▶ Preview` CodeLens above every `@Preview`-annotated method in Java files, and renders the returned HTML in a side-panel WebView.

## VSCode Plugin

https://marketplace.visualstudio.com/items?itemName=RebelCraft.java-html-toolkit

## Prerequisites

- [Maven](https://maven.apache.org/) 3.9+ or the repository `mvnw` wrapper
- [Node.js](https://nodejs.org/) 18+
- [VS Code](https://code.visualstudio.com/) 1.85+
- [Java](https://adoptium.net/) 17+
- [Language Support for Java by Red Hat](https://marketplace.visualstudio.com/items?itemName=redhat.java)

The extension declares `redhat.java` as a dependency and bundles Maven for Java / Project Manager for Java in its extension pack metadata so the expected Java workspace tooling is installed alongside it. The extension itself is built through the root Maven reactor, while its npm scripts remain the local implementation detail.

## Settings

- `java-html-toolkit.cssFiles`: CSS files/globs/URLs to inject into preview output.
- `java-html-toolkit.inlineStyles`: Inline CSS appended to preview output.
- `java-html-toolkit.buildStrategy`: `maven-first` (default) or `java-first`.
    - `maven-first` tries Maven commands first and falls back to Java extension APIs.
    - `java-first` tries Java extension APIs first and falls back to Maven commands.
- `java-html-toolkit.debugLogs`: when `true`, logs classpath/build decision paths and fallback reasons.

## Commands

- `Java HTML: Open Preview` — open or refresh preview for the selected `@Preview` method.
- `Java HTML: Show Diagnostics` — opens a markdown report with current settings, active previews, and per-project cache state.

## Running the extension in development mode

Development mode lets you test the extension on any project without packaging or publishing it.

### 1. Build the Java modules

The extension calls `PreviewRunner` at runtime, so the Java modules should be built from the root reactor first.

```bash
./mvnw test
```

### 2. Install extension dependencies and compile

```bash
cd vscode-extension
npm install
npm run compile
```

### 3. Launch the Extension Development Host

Press **F5** (or go to **Run → Start Debugging**) from any workspace folder.

VS Code opens a second window labelled **[Extension Development Host]** with the extension loaded from your local source. Any change you make followed by `npm run compile` (or `npm run watch` for automatic recompilation) is reflected immediately after reloading the host window (**Ctrl+R** / **Cmd+R**).

> **Note:** You can work from either the root `java-html-toolkit` folder or open just the `vscode-extension` subfolder—both work. The root folder includes a `.vscode/launch.json` configuration that automatically targets the extension subfolder.

### 4. Test on an example project

Inside the Extension Development Host window, open a Maven project that depends on `java-html-toolkit-preview`:

```xml
<!-- in your project's pom.xml -->
<dependency>
    <groupId>io.github.teggr</groupId>
    <artifactId>java-html-toolkit-preview</artifactId>
    <version>1.0.3-SNAPSHOT</version>
</dependency>
```

Then annotate a no-arg method with `@Preview`:

```java
import io.github.teggr.html.preview.Preview;

public class MyPreviews {

    @Preview("Main layout")
    public String mainLayout() {
        return "<h1>Hello, Java HTML Tooling!</h1>";
    }
}
```

Open the Java file in the editor — a **▶ Preview** CodeLens appears above the method. Click it to compile the project and render the HTML output in a panel beside the editor.

If `@Preview("...")` includes a label, that value is used as the preview panel title; otherwise the method name is used.

#### Previewing j2html components

j2html rendering is provided by the optional
[`j2html-toolkit-preview` adapter](https://github.com/teggr/j2html-toolkit/tree/main/preview),
not by the generic preview runtime or VS Code extension. Add the generic runtime
and the adapter to the example project's `pom.xml`. Select a published adapter
version that is compatible with the chosen runtime version; the placeholder
below must be replaced before building.

```xml
<properties>
    <java-html-toolkit.version>1.0.3</java-html-toolkit.version>
    <j2html-toolkit.version>COMPATIBLE_PUBLISHED_VERSION</j2html-toolkit.version>
</properties>

<dependencies>
    <dependency>
        <groupId>io.github.teggr</groupId>
        <artifactId>java-html-toolkit-preview</artifactId>
        <version>${java-html-toolkit.version}</version>
    </dependency>
    <dependency>
        <groupId>io.github.teggr</groupId>
        <artifactId>j2html-toolkit-preview</artifactId>
        <version>${j2html-toolkit.version}</version>
    </dependency>
</dependencies>
```

The adapter also brings in its j2html dependency. A preview method can return
j2html `DomContent`:

```java
import static j2html.TagCreator.div;

import io.github.teggr.html.preview.Preview;
import j2html.tags.DomContent;

public class MyPreviews {

    @Preview("Main layout")
    public DomContent mainLayout() {
        return div("Hello from j2html");
    }
}
```

The adapter is discovered through `PreviewGenerator`'s Java `ServiceLoader`
registration when the project runs `PreviewRunner`; the extension needs no
j2html-specific configuration. Without the adapter, the generic runtime
supports its built-in `String` and object-return behavior, but does not render
j2html `DomContent`.

Once a preview is open:

- saving the preview source file reruns the preview after a short debounce
- saving configured CSS files rerenders the existing HTML without rerunning Maven or Java
- changing `pom.xml` invalidates the cached classpath so the next preview refresh resolves dependencies again

### 5. Iterating quickly

Run the TypeScript compiler in watch mode so recompilation happens on every save:

```bash
npm run watch
```

To build or package the extension from the repository root, use Maven:

```bash
./mvnw -pl vscode-extension package
```

That produces the VSIX from the Maven-owned version and lifecycle.

After saving a change to `src/extension.ts`, reload the Extension Development Host window (**Ctrl+R** / **Cmd+R**) to pick up the new build.

## How it works

1. **CodeLens** — `PreviewCodeLensProvider` asks VS Code's Java symbol provider for classes and methods, then matches `@Preview` annotations from source text onto those symbols before registering a `▶ Preview` lens for each match.
2. **Build** — clicking the lens compiles using the configured strategy (`java-html-toolkit.buildStrategy`): Java extension first or Maven first, with fallback to the other path when needed. The compile step is cached until a Java or `pom.xml` change invalidates it.
3. **Classpath** — classpath resolution also follows `java-html-toolkit.buildStrategy`, with fallback between Java extension classpath and Maven dependency classpath. The resolved classpath is cached and recomputed when `pom.xml` changes.
4. **Run** — `java -cp <classpath> io.github.teggr.html.preview.PreviewRunner <className> <methodName>` is executed and its stdout (the rendered HTML) is displayed in the WebView panel.
5. **Refresh** — when CSS changes, the extension reuses the last rendered HTML and only reinjects styles into the WebView instead of rerunning the preview method.

## `@Preview` contract and verification

Discovery recognizes `@Preview` and the positional label form `@Preview("label")` on their own source line. Preview methods must be no-argument, non-`void` instance methods on a class with a no-argument constructor. Any visibility is accepted. `String` results are rendered directly; other non-`void` results use `toString()` unless a `PreviewGenerator` SPI implementation handles the result. A `null` or empty result displays the “method returned no output” placeholder.

The regex fallback handles only the first top-level class and conventional one-line annotation/method declarations. Fully qualified annotation names and multi-line annotation arguments are not recognized by fallback discovery. The panel label is extracted from `@Preview("label")`; other annotation attributes remain available to Java-side preview generators but are not used by the VS Code panel title.

The extension's automated tests cover symbol and regex discovery (including partial symbol responses and changed source text), Maven reactor selection and module dependency inclusion, runtime strategy ordering and cache invalidation, and CSS injection into fragments and full documents. The Maven fixtures in `test-fixtures/` provide clean single-module and multi-module projects; install the preview runtime first, then run the fixture builds from the repository root:

```powershell
.\mvnw.cmd -pl preview install
.\mvnw.cmd -f vscode-extension\test-fixtures\single-module\pom.xml clean test-compile dependency:build-classpath '-Dmdep.outputFile=${project.build.directory}/java-html-toolkit-classpath.txt' '-Dmdep.includeScope=test' -q
.\mvnw.cmd -f vscode-extension\test-fixtures\multi-module\pom.xml -pl preview-app -am clean test-compile dependency:build-classpath '-Dmdep.outputFile=${project.build.directory}/java-html-toolkit-classpath.txt' '-Dmdep.includeScope=test' -q
```

| Scenario | Verification | Evidence |
| --- | --- | --- |
| Clean single-module Maven workspace | Build and run the single-module fixture's `@Preview` method. | Verified: `PreviewRunner` returned `<main>single module</main>`. |
| Multi-module reactor with sibling dependency | Build the multi-module fixture using `-pl preview-app -am` and run `MultiPreview.render()`. | Verified: `PreviewRunner` returned `<h1>multi module</h1>`, including the sibling module on the runtime classpath. |
| Source edits and clean builds | The extension tests rediscovery against changed source; for the live check, open a fixture, preview a method, save an edit, then run `clean` and preview again. | Discovery test automated; watcher and panel refresh require the Extension Development Host. |
| Dependency changes and classpath invalidation | Change a fixture dependency in its `pom.xml`, save it, and refresh the preview. The next diagnostics report should show refreshed compile/classpath state. | POM invalidation policy automated; extension host check is manual. |
| Compilation failure | Introduce a Java syntax error in a fixture preview and refresh it. The panel should show the build failure details instead of retaining stale rendered HTML; restore the source afterward. | Error display requires the Extension Development Host. |
| Runtime and classpath fallbacks | With Red Hat Java installed, try both `maven-first` and `java-first`. Enable `java-html-toolkit.debugLogs` to see failed strategies and fallback reasons. | Resolver ordering and fallback policy automated; Java extension integration is manual. |
| Panel reuse and CSS | Invoke the same preview twice and confirm the existing panel is revealed. Change the fixture CSS and confirm styles update without rerunning the method; also try `inlineStyles` and a CSS file/glob setting. | CSS insertion automated; file resolution and panel reuse require the Extension Development Host. |

Panel reuse, Maven/Java host integration, and watcher-driven refreshes require the VS Code Extension Development Host and are manual checks; pure discovery, reactor selection, cache policy, and CSS insertion are covered by the extension tests. If both runtime strategies fail, the panel displays the collected build or classpath errors rather than stale preview output.

## Release and installation

- **VS Code Marketplace**: install from the Marketplace listing for automatic update delivery.
- **Direct VSIX**: download the `.vsix` attached to each GitHub Release and install via **Extensions: Install from VSIX...**.

The GitHub Actions release workflow publishes the Marketplace extension and attaches
the matching `.vsix` to the versioned GitHub Release. Marketplace and GitHub Release
distribution are part of the same manually dispatched release process.

Build ownership is Maven-first for the repository; npm remains the extension-local implementation detail.

Publishing information - https://marketplace.visualstudio.com/manage/publishers/rebelcraft 

Helper guides - https://dev.to/diana_tang/complete-guide-publishing-vs-code-extensions-to-both-marketplaces-4d58 and https://code.visualstudio.com/api/working-with-extensions/publishing-extension#publishing-extensions