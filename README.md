# java-html-toolkit

Toolkit for developing, previewing, verifying and testing HTML-producing Java code.

## Modules

- `vscode-extension`: VS Code extension that discovers `@Preview` methods, compiles projects, and renders HTML. It is built from the root Maven reactor and keeps its npm scripts as the local implementation detail.
- `html-preview`: runtime entrypoint (`PreviewRunner`) for (`@Preview`), extensible by supporting template frameworks.
- `test-docs`: publishable documentation/test support module for preview examples and related HTML tooling fixtures.

## Architecture seam

This repository now separates the preview contract from templating adapters:

- The extension only depends on the generic preview API runtime contract.
- j2html-specific implementation/examples live in a dedicated adapter module.
- Additional adapters (for example Thymeleaf/JTE) can be implemented independently using the same API.

## Release automation

Releases are currently driven from local/manual workflows:

- Unified Maven + VS Code release flow: `docs/RELEASE.md`

## Build entry points

- `./mvnw test` runs the full repository build.
- `./mvnw -pl html-preview install` installs the preview API into the local Maven repository.
- `./mvnw -Pcentral -pl html-preview,html-test-docs package` prepares the publishable Java modules with sources, javadocs, and signing configured through the root reactor.
- `./mvnw -pl vscode-extension package` compiles, tests, and packages the extension into a VSIX.

Versioning is Maven-driven: the root reactor version is the source of truth, and `vscode-extension/package.json` follows that version.
