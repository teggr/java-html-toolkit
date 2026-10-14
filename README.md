# java-html-toolkit

Toolkit for developing, previewing, verifying and testing HTML-producing Java code.

## Modules

- `vscode-extension`: VS Code extension that discovers `@Preview` methods, compiles projects, and renders HTML. It is built from the root Maven reactor and keeps its npm scripts as the local implementation detail.
- `preview` (`java-html-toolkit-preview`): annotation (`@Preview`) and runtime entrypoint (`PreviewRunner`), extensible via the `PreviewGenerator` SPI.
- `preview-thymeleaf` (`java-html-toolkit-preview-thymeleaf`): optional Thymeleaf `PreviewGenerator` adapter for classpath templates and model data.
- `preview-jte` (`java-html-toolkit-preview-jte`): optional JTE `PreviewGenerator` adapter for classpath templates and typed parameters.
- `examples` (`java-html-toolkit-examples`): nested sample applications, including a Spring Boot Thymeleaf bookstore and a small JTE webapp.
- `html-catalog` (`java-html-toolkit-html-catalog`): scans the compiled classpath for `@Preview` methods and generates a self-contained, browsable static HTML catalog.
- `test-docs`: publishable documentation/test support module for preview examples and related HTML tooling fixtures.

## `@Preview` annotation

Mark any no-argument method with `@Preview` to register it as a component preview:

```java
@Preview(value = "Primary Button", group = "Buttons", description = "A primary action button", order = 1)
public String primaryButton() {
    return "<button class=\"btn-primary\">Click me</button>";
}
```

| Attribute     | Default              | Description                                            |
|---------------|----------------------|--------------------------------------------------------|
| `value`       | method name          | Display name shown in the VS Code panel and catalog    |
| `group`       | declaring class name | Catalog group (component family) for sidebar grouping  |
| `description` | *(empty)*            | Prose description shown below the preview name         |
| `order`       | `0`                  | Sort order within the group; ties break by name, then declaring class and method signature |

## HTML Component Catalog

Add `java-html-toolkit-html-catalog` to the project containing the compiled
`@Preview` methods. The scanner examines classpath directories (including the
project's `target/classes`) and intentionally skips JARs, so run generation in
each module whose own compiled classes should appear in the catalog.

```xml
<dependency>
    <groupId>io.github.teggr</groupId>
    <artifactId>java-html-toolkit-html-catalog</artifactId>
    <version>1.0.3</version>
</dependency>
```

Bind `CatalogRunner` to Maven's `verify` phase with the `exec-maven-plugin`:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>generate-catalog</id>
            <phase>verify</phase>
            <goals><goal>java</goal></goals>
            <configuration>
                <mainClass>io.github.teggr.html.catalog.CatalogRunner</mainClass>
                <arguments>
                    <argument>--output-dir</argument>
                    <argument>${project.build.directory}/html-catalog</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Run `./mvnw verify` (or `mvn verify` in a consumer project) and open
`target/html-catalog/index.html` in a browser. The output directory contains the
catalog shell plus a `frames/` tree with one isolated HTML page per preview.
The shell lists groups and components beside a live iframe preview; its
**Source** tab shows the rendered HTML markup, not the Java method source.

The default output directory is `target/html-catalog`. Set `--output-dir` in
the plugin arguments to choose another path. From the consumer project root,
generate on demand without a phase binding by running:

```
./mvnw org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=io.github.teggr.html.catalog.CatalogRunner \
  -Dexec.args="--output-dir target/html-catalog"
```

Unsupported annotated methods (for example, methods with parameters or `void`
return type) remain visible as individual entries with a render-error message.
An exception in one preview is likewise reported on that entry and does not
prevent other previews from rendering. Repeated discovery of the same
declaring-class method is deduplicated; different methods with the same display
name remain separate entries.

The catalog shell's CSS and JavaScript are embedded in `index.html`; there are
no external shell assets or catalog-theme settings. Preview-specific styling
can be returned inline in the preview HTML (for example, with a `<style>` tag).
The generator does not copy project CSS, images, fonts, or other resources into
the output. If a preview references external assets, make their URLs reachable
from the generated frame pages and include those assets when publishing the
catalog. For a static deployment, publish the complete `html-catalog/`
directory, plus any referenced preview assets, to any static file host. You
can also share the generated directory as files; no server-side runtime is
required. Changes to the catalog shell itself require changing the generator
template; regenerating replaces the generated files.

## Architecture seam

This repository owns the framework-neutral preview contract and runtime. It does
not bundle framework-specific template engines:

- `preview` (`java-html-toolkit-preview`) provides `@Preview`, `PreviewRunner`,
  and the `PreviewGenerator` SPI. Built-in generators handle `String` and
  other object return values.
- The j2html adapter is maintained separately in
  [`teggr/j2html-toolkit`, module `preview`](https://github.com/teggr/j2html-toolkit/tree/main/preview)
  and published as `io.github.teggr:j2html-toolkit-preview`. Add that optional
  dependency to a project whose `@Preview` methods return j2html
  `DomContent`; the adapter supplies the j2html-specific rendering behavior.
- The Thymeleaf adapter is provided in this repository as the optional
  `preview-thymeleaf` module and published as
  `io.github.teggr:java-html-toolkit-preview-thymeleaf`; it supports
  `ThymeleafPreview` values containing a classpath template name and model.
- The optional `preview-jte` module is published as
  `io.github.teggr:java-html-toolkit-preview-jte`; it supports `JtePreview`
  values containing a classpath template path and named parameters.
- The VS Code extension invokes the generic `PreviewRunner`; it does not bundle
  j2html or require any particular adapter. Only add the adapters your project
  uses, and use adapter releases documented as compatible with the selected
  `java-html-toolkit-preview` version.

### Thymeleaf adapter

Add the optional adapter to a project whose `@Preview` methods return a
`ThymeleafPreview`. It brings in the generic preview API and Thymeleaf engine;
projects that do not use this adapter do not acquire a Thymeleaf dependency.

```xml
<dependency>
    <groupId>io.github.teggr</groupId>
    <artifactId>java-html-toolkit-preview-thymeleaf</artifactId>
    <version>1.0.3</version>
</dependency>
```

The default engine resolves HTML templates from
`src/main/resources/templates/`, using the template name without the
`.html` suffix. Provide template variables as a `Map<String, Object>`:

```java
import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.thymeleaf.ThymeleafPreview;

import java.util.Map;

public class GreetingPreview {
    @Preview("Greeting")
    public ThymeleafPreview greeting() {
        return new ThymeleafPreview("greeting", Map.of("name", "Ada"));
    }
}
```

For this example, create `src/main/resources/templates/greeting.html`:

```html
<main>Hello, <span th:text="${name}">friend</span>!</main>
```

When `java-html-toolkit-preview-thymeleaf` is on the runtime classpath,
`PreviewRunner` discovers the adapter through `ServiceLoader` and renders the
template using the returned model. No custom engine setup or VS Code extension
configuration is required.

### JTE adapter

Add the optional adapter (which transitively includes the preview API and JTE):

```xml
<dependency>
    <groupId>io.github.teggr</groupId>
    <artifactId>java-html-toolkit-preview-jte</artifactId>
    <version>1.0.3</version>
</dependency>
```

Return `io.github.teggr.html.preview.jte.JtePreview` from a no-argument
`@Preview` instance method on a class with a no-argument constructor:

```java
import io.github.teggr.html.preview.Preview;
import io.github.teggr.html.preview.jte.JtePreview;
import java.util.Map;

public class GreetingPreview {
    @Preview("Greeting")
    public JtePreview greeting() {
        return new JtePreview("greeting.jte", Map.of("name", "Ada"));
    }
}
```

Create `src/main/resources/templates/greeting.jte`:

```html
@param String name
<main>Hello, ${name}!</main>
```

Template paths are relative to the classpath's `templates/` directory and
include the `.jte` suffix (for example, `pages/greeting.jte`). Map keys match
JTE's typed `@param` names; values must have the declared types. Model classes
and their enclosing classes must be public, with public members accessed by
templates. Null values are permitted; the parameter map is defensively copied.
JTE HTML mode escapes interpolated values.

The default embedded engine compiles source templates on demand using a
**JDK 17+**, not a JRE. Maven's standard resource copying puts templates on the
runtime classpath, including when packaged in a JAR; no JTE Maven plugin,
Spring context, custom engine, or extension configuration is required. Generated
classes use a fresh temporary directory that is removed after rendering.
Templates and model classes must be available on the preview runtime classpath.
Templates are trusted Java code, not a sandbox for user-supplied content.

This first version deliberately supports classpath source templates only,
not application-specific engines or precompiled-only templates. Keep source
templates in resources even if the application separately precompiles JTE for
production. `ServiceLoader` discovers the adapter automatically; the generic
runtime and VS Code extension remain JTE-free unless the project adds this
dependency. Existing String previews continue to work.

### Implementing another adapter

An adapter can be maintained in its own module or repository. Depend on
`java-html-toolkit-preview` and the relevant template engine there, then
implement `io.github.teggr.html.preview.spi.PreviewGenerator`. Its `supports`
method should identify the return types it can render, and `generate` should
invoke the preview method and return the rendered HTML. Register the
implementation with Java's `ServiceLoader` by adding a
`META-INF/services/io.github.teggr.html.preview.spi.PreviewGenerator` resource
containing the implementation's fully qualified class name. Higher
`priority()` values take precedence over the built-in string and object
generators. Keeping the engine dependency in the adapter module leaves it
optional for other users of the generic preview runtime.

## Example applications

The `examples` Maven aggregator contains independently buildable sample
applications. The `examples/thymeleaf` module is a Spring Boot bookstore with
home and book-detail pages, shared Thymeleaf fragments, and `@Preview` methods
for each page and reusable component. From the repository root, run:

```sh
./mvnw -pl examples/thymeleaf -am package
java -jar examples/thymeleaf/target/java-html-toolkit-examples-thymeleaf-1.0.3-SNAPSHOT.jar
```

Open `http://localhost:8080` for the bookstore. The sample previews can also be
run by the VS Code extension when the example module is opened as a Maven
project; its previews use the same classpath templates as the running app.

The `examples/jte` module serves a JTE welcome page with the JDK's embedded
HTTP server. It uses the same `GreetingPreviews#welcome` method, template, and
parameters for both the webapp and preview. From the repository root:

```sh
./mvnw -pl examples/jte -am install
./mvnw -pl examples/jte exec:java \
  -Dexec.mainClass=io.github.teggr.examples.jte.GreetingApplication
```

Open `http://localhost:8080` (loopback only); stop with Ctrl+C. To execute its
preview directly:

```sh
./mvnw -pl examples/jte exec:java \
  -Dexec.mainClass=io.github.teggr.html.preview.PreviewRunner \
  -Dexec.args="io.github.teggr.examples.jte.GreetingPreviews welcome"
```

Open `examples/jte` as a Maven project in VS Code to use its preview CodeLens.

## Release automation

Releases are prepared and published through GitHub Actions:

- CI checks: `.github/workflows/maven.yml`
- Manually dispatched Maven Central, VS Code Marketplace, and GitHub Release flow: `.github/workflows/release.yml`
- Release prerequisites and verification: `docs/RELEASE.md`

## Build entry points

- `./mvnw test` runs the full repository build.
- `./mvnw -pl preview install` installs the preview API into the local Maven repository.
- `./mvnw -Pcentral -pl preview,preview-thymeleaf,preview-jte,test-docs,html-catalog package` prepares the publishable Java modules with sources, javadocs, and signing configured through the root reactor.
- `./mvnw -pl vscode-extension package` compiles, tests, and packages the extension into a VSIX.

Versioning is Maven-driven: the root reactor version is the source of truth, and `vscode-extension/package.json` follows that version.
