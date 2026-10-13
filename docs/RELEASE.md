# Unified Release Process

This document describes the GitHub Actions release workflow for this repository.

Maven is the source of truth for release versioning. The manually dispatched
`.github/workflows/release.yml` workflow publishes Java artifacts to Maven Central,
publishes the VS Code extension to the Marketplace, creates a GitHub Release with
the VSIX attached, and then commits the next snapshot version to the selected branch.

## Scope

Java artifacts published to Maven Central:
- io.github.teggr:java-html-toolkit (parent POM)
- io.github.teggr:java-html-toolkit-preview
- io.github.teggr:java-html-toolkit-test-docs
- io.github.teggr:java-html-toolkit-html-catalog

VS Code extension:
- RebelCraft.java-html-toolkit

## Prerequisites

- Java 17+
- Node.js 18+
- A Maven Central publishing token and namespace access for `io.github.teggr`.
- A GPG signing key available as a private-key export and its passphrase.
- VS Code Marketplace publisher access for `RebelCraft`, with a PAT allowed to publish extensions.
- GitHub Actions allowed to write repository contents and push to the selected release branch. Branch protection must permit the workflow's version-bump commit.

Configure these repository Actions secrets:

| Secret | Purpose |
| --- | --- |
| `CENTRAL_USERNAME` | Sonatype Central publishing token username |
| `CENTRAL_PASSWORD` | Sonatype Central publishing token password |
| `GPG_KEY_ID` | Signing key ID used by the Maven GPG plugin |
| `GPG_PRIVATE_KEY` | ASCII-armored private key used to sign artifacts |
| `GPG_PASSPHRASE` | Passphrase for the signing key |
| `VSCE_PAT` | Marketplace publishing token for the `RebelCraft` publisher |

## CI

Pull requests and pushes to `main` run the Maven reactor tests, VS Code extension
lint, and extension packaging in `.github/workflows/maven.yml`.

## Release flow

1. Merge the release-ready code to the branch from which the release will run. Keep
   the root Maven version and `vscode-extension/package.json` on the same
   `-SNAPSHOT` version (currently `1.0.3-SNAPSHOT`).
2. Configure all required repository secrets and ensure the selected branch allows
   the workflow to push the next-snapshot commit.
3. In GitHub Actions, run **Publish release** using **Run workflow** and select that
   branch. The release version is derived by removing `-SNAPSHOT` from the root
   Maven version; no publishing workflow should be run for an existing release tag.
4. The workflow runs Maven and extension checks, validates Maven Central metadata,
   aligns the extension version, packages the VSIX, deploys Java modules, publishes
   the extension, and creates a GitHub Release with the VSIX attached.
5. After successful publication, the workflow advances Maven and VS Code versions
   to the next `-SNAPSHOT` and pushes that change to the selected branch.

For the current release this produces version `1.0.3`, followed by
`1.0.4-SNAPSHOT` on the release branch.

## Outputs

- Maven Central publication of the parent POM, preview, test-docs, and HTML catalog Java artifacts.
- Marketplace publication of `RebelCraft.java-html-toolkit`.
- GitHub Release tagged with the release version and containing the packaged VSIX.

## Notes

- Maven Central and Marketplace do not provide a shared transaction. If one channel
  accepts a release and another fails, do not rerun the entire workflow blindly:
  published versions are immutable. Resolve the partially published release before
  retrying, and only republish the extension version if Marketplace has not accepted it.
- The workflow does not run automatically on tags; publication requires an explicit
  manual dispatch from a branch.
- The README's dependency example uses the latest intended release version, not the
  reactor's development snapshot.
