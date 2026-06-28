# java-html-tooling

Tooling for developing, previewing, verifying and testing HTML-producing Java code.

## Modules

- `java-html-preview-api`: generic Java preview contract (`@Preview`) and runtime entrypoint (`PreviewRunner`).
- `java-html-preview-j2html`: j2html adapter/examples built on top of `java-html-preview-api`.
- `java-html-tooling-vscode`: VS Code extension that discovers `@Preview` methods, compiles projects, and renders HTML.

## Architecture seam

This repository now separates the preview contract from templating adapters:

- The extension only depends on the generic preview API runtime contract.
- j2html-specific implementation/examples live in a dedicated adapter module.
- Additional adapters (for example Thymeleaf/JTE) can be implemented independently using the same API.

## Release automation

Releases are currently driven from local/manual workflows:

- Maven Central release flow: `docs/RELEASE-MAVEN.md`
- VS Code extension release flow: `docs/RELEASE-VSCODE.md`
