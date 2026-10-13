# java-html-toolkit

Toolkit for developing, previewing, verifying and testing HTML-producing Java code.

## Modules

- `vscode-extension`: VS Code extension that discovers `@Preview` methods, compiles projects, and renders HTML. It is built from the root Maven reactor and keeps its npm scripts as the local implementation detail.
- `preview` (`java-html-toolkit-preview`): annotation (`@Preview`) and runtime entrypoint (`PreviewRunner`), extensible via the `PreviewGenerator` SPI.
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
| `order`       | `0`                  | Sort order within the group; ties break alphabetically |

## HTML Component Catalog

Add `java-html-toolkit-html-catalog` to your project's dependencies, then configure the
`exec-maven-plugin` to run `CatalogRunner` after the `test` phase:

```xml
<dependency>
    <groupId>io.github.teggr</groupId>
    <artifactId>java-html-toolkit-html-catalog</artifactId>
    <version>1.0.3</version>
</dependency>
```

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>generate-catalog</id>
            <phase>verify</phase>
            <goals><goal>java</goal></goals>
            <configuration>
                <mainClass>io.github.teggr.html.catalog.CatalogRunner</mainClass>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Run `./mvnw verify` and open `target/html-catalog/index.html` in a browser.
The catalog shows a two-panel view: a sidebar listing all groups and components on the
left, and a live iframe preview on the right with a **Source** tab.

You can also run the catalog generator on-demand:

```
mvn exec:java -Dexec.mainClass=io.github.teggr.html.catalog.CatalogRunner \
              -Dexec.args="--output-dir target/html-catalog"
```

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
- The VS Code extension invokes the generic `PreviewRunner`; it does not bundle
  j2html or require any particular adapter. Only add the adapters your project
  uses, and use adapter releases documented as compatible with the selected
  `java-html-toolkit-preview` version.

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

## Release automation

Releases are prepared and published through GitHub Actions:

- CI checks: `.github/workflows/maven.yml`
- Manually dispatched Maven Central, VS Code Marketplace, and GitHub Release flow: `.github/workflows/release.yml`
- Release prerequisites and verification: `docs/RELEASE.md`

## Build entry points

- `./mvnw test` runs the full repository build.
- `./mvnw -pl preview install` installs the preview API into the local Maven repository.
- `./mvnw -Pcentral -pl preview,test-docs,html-catalog package` prepares the publishable Java modules with sources, javadocs, and signing configured through the root reactor.
- `./mvnw -pl vscode-extension package` compiles, tests, and packages the extension into a VSIX.

Versioning is Maven-driven: the root reactor version is the source of truth, and `vscode-extension/package.json` follows that version.
