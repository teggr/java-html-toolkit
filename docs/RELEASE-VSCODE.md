# Release VS Code Extension (Local)

This document describes local packaging and publishing for the extension.

Extension ID:
- RebelCraft.java-html-tooling

## Prerequisites

- Node.js 18+
- npm
- VS Code Extension Manager (vsce) available via dev dependency
- Marketplace publisher created: RebelCraft (https://marketplace.visualstudio.com/manage/publishers/RebelCraft)

## Build and verify locally

```bash
cd java-html-tooling-vscode
npm ci
npm test
npm run package
```

Output:
- java-html-tooling-<version>.vsix

## Install VSIX locally for verification

```bash
code --install-extension ./java-html-tooling-<version>.vsix --force
```

## Publish directly to VS Code Marketplace

One-time login on your machine:

```bash
cd java-html-tooling-vscode
npx vsce login RebelCraft
```

Publish current package.json version:

```bash
npx vsce publish
```

Or publish a specific packaged VSIX:

```bash
npx vsce publish --packagePath ./java-html-tooling-<version>.vsix
```

## Suggested release flow

```bash
cd java-html-tooling-vscode
npm version <patch|minor|major>
npm test
npm run package
npx vsce publish
```

## Notes

- PAT must be created at https://dev.azure.com/rebelcraft with Marketplace → Manage scope.
- If publish fails after version bump, fix the issue and publish the same version if it has not been accepted by Marketplace.
