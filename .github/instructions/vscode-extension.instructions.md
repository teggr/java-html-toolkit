---
description: "Use when editing the VS Code extension in j2html-preview-vscode (commands, CodeLens discovery, preview runtime, settings, packaging, or docs). Enforces reliability fallbacks, test-first updates, and release checks."
name: "VS Code Extension Principles"
applyTo:
  - "j2html-preview-vscode/src/**/*.ts"
  - "j2html-preview-vscode/package.json"
  - "j2html-preview-vscode/README.md"
  - "j2html-preview-vscode/.vscodeignore"
---
# VS Code Extension Principles

- Keep preview execution resilient.
  - Preserve dual-path runtime behavior: Java extension path and Maven fallback path.
  - Never remove both fallback paths in the same change.

- Keep preview discovery resilient.
  - Keep symbol-based discovery as primary.
  - Keep regex fallback available when symbols are missing or partial.

- Treat runtime policy as the source of truth.
  - Use `runtimePolicy` helpers for strategy ordering and invalidation rules instead of duplicating logic in `extension.ts`.

- Add or update tests for every behavior change.
  - Add tests under `j2html-preview-vscode/src/test/` for new behavior.
  - Include regressions for fallback behavior when fixing production issues.
  - Keep tests deterministic and focused on pure logic where possible.

- Keep extension wiring complete when adding features.
  - For new commands: update command contribution in `package.json`, register in `extension.ts`, and document in `README.md`.
  - For new settings: add configuration in `package.json`, read in runtime code, and document in `README.md`.

- Preserve packaging hygiene.
  - Do not ship test artifacts in VSIX output.
  - Keep `.vscodeignore` aligned with build output changes.

- Validate before handing off.
  - Run `npm test`, `npm run compile`, and `npm run package` in `j2html-preview-vscode` after extension changes.
