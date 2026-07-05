# Unified Release Process (Maven-Controlled)

This document defines the single release workflow for this repository.

Maven is the source of truth for versioning and release orchestration:
- Java artifacts are published to Maven Central from the root reactor.
- The VS Code extension is packaged from the root reactor and published to Marketplace.

## Scope

Java artifacts:
- dev.rebelcraft:java-html-toolkit-html-preview
- dev.rebelcraft:java-html-toolkit-html-test-docs

VS Code extension:
- RebelCraft.java-html-toolkit

## Prerequisites

- Java 17+
- Maven 3.9+ (or `./mvnw`)
- Node.js 18+
- npm
- GPG key configured locally
- Sonatype Central credentials in `~/.m2/settings.xml`
- VS Code Marketplace publisher access for RebelCraft

Expected Maven server entries:
- `central` (username/token for Sonatype Central)
- `<gpg.keyname>` (passphrase for your GPG key)

## One-time VS Code Marketplace setup

```bash
cd vscode-extension
npx vsce login RebelCraft
```

PAT note:
- Create a PAT at https://dev.azure.com/rebelcraft with Marketplace -> Manage scope.

## Unified Release Flow

```bash
# 1) Start from a clean working tree at repo root
cd java-html-toolkit
git status
./mvnw clean

# 2) Create the release tag (before version mutation)
git tag <version>

# 3) Set release version across the reactor (remove -SNAPSHOT)
./mvnw -Pcentral versions:set -DremoveSnapshot -DprocessAllModules

# 4) Validate build and metadata
./mvnw test
./mvnw -Pcentral org.kordamp.maven:pomchecker-maven-plugin:1.14.0:check-maven-central

# 5) Publish Java artifacts to Maven Central
./mvnw -Pcentral deploy -DignorePublishedComponents=true

# 6) Package the VS Code extension using Maven-owned workflow
./mvnw -pl vscode-extension package

# 7) Publish extension to Marketplace (from package.json version synced by Maven)
cd vscode-extension
npx vsce publish
cd ..

# 8) Move to next snapshot
./mvnw -Pcentral versions:set -DnextSnapshot -DprocessAllModules
./mvnw -Pcentral versions:commit

# 9) Commit and push version changes + tag
git add pom.xml html-preview/pom.xml html-test-docs/pom.xml vscode-extension/package.json
git commit -m "release(java-html-toolkit): <version>"
git push --follow-tags
```

## Outputs

- Maven Central publication for Java modules.
- VSIX generated under `vscode-extension/` during package phase.
- Marketplace publication for `RebelCraft.java-html-toolkit`.

## Notes

- Do not reuse a release version if Maven Central publication partially succeeds.
- If Marketplace publish fails, fix and republish the same version only if Marketplace has not accepted it.
- Root `deploy` is for repository publishing flow; extension module publishing is `package` + `vsce publish`.
