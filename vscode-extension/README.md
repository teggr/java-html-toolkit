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

Inside the Extension Development Host window, open a Maven project that depends on `java-html-preview-api`:

```xml
<!-- in your project's pom.xml -->
<dependency>
    <groupId>dev.rebelcraft</groupId>
    <artifactId>java-html-preview-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Then annotate a no-arg method with `@Preview`:

```java
import dev.rebelcraft.html.preview.Preview;

public class MyPreviews {

    @Preview("Main layout")
    public String mainLayout() {
        return "<h1>Hello, Java HTML Tooling!</h1>";
    }
}
```

Open the Java file in the editor — a **▶ Preview** CodeLens appears above the method. Click it to compile the project and render the HTML output in a panel beside the editor.

If `@Preview("...")` includes a label, that value is used as the preview panel title; otherwise the method name is used.

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
4. **Run** — `java -cp <classpath> dev.rebelcraft.html.preview.PreviewRunner <className> <methodName>` is executed and its stdout (the rendered HTML) is displayed in the WebView panel.
5. **Refresh** — when CSS changes, the extension reuses the last rendered HTML and only reinjects styles into the WebView instead of rerunning the preview method.

## Release and installation

- **VS Code Marketplace**: install from the Marketplace listing for automatic update delivery.
- **Direct VSIX**: download the `.vsix` attached to each GitHub Release and install via **Extensions: Install from VSIX...**.

Repository workflows support both channels: GitHub Release artifacts are always produced, and Marketplace publishing is opt-in via the extension module's publish step.

Build ownership is Maven-first for the repository; npm remains the extension-local implementation detail.

Publishing information - https://marketplace.visualstudio.com/manage/publishers/rebelcraft 

Helper guides - https://dev.to/diana_tang/complete-guide-publishing-vs-code-extensions-to-both-marketplaces-4d58 and https://code.visualstudio.com/api/working-with-extensions/publishing-extension#publishing-extensions