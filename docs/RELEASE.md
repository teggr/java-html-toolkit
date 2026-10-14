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
- io.github.teggr:java-html-toolkit-preview-thymeleaf
- io.github.teggr:java-html-toolkit-preview-jte
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
lint, and extension packaging in `.github/workflows/maven.yml`. The reactor test
also runs the extension's compiled tests; packaging runs the extension compile
and VSIX packaging steps.

## Release flow

1. Merge release-ready code to the branch from which the release will run. Before
   dispatch, confirm the latest run of `.github/workflows/maven.yml` passed on that
   branch. Keep the root Maven version and `vscode-extension/package.json` on the
   same `-SNAPSHOT` version (currently `1.0.3-SNAPSHOT`).
2. Configure all required repository secrets and ensure branch protection allows
   GitHub Actions to push both the release-version commit and the next-snapshot
   commit to the selected branch.
3. In GitHub Actions, run **Publish release** using **Run workflow** and select that
   branch. The workflow only accepts a branch whose Maven version ends in
   `-SNAPSHOT`, derives the release version by removing that suffix, and refuses
   to run if the release tag already exists.
4. The workflow runs the Maven reactor tests (including the extension's compiled
   tests), checks release-version alignment, validates Maven Central metadata,
   lints the extension, and packages a VSIX. It then publishes the Java modules
   and the VSIX to their respective registries.
5. Once both publications succeed, the workflow commits and pushes the release
   versions to the selected branch. It creates the GitHub Release and tag at that
   release-version commit, with the VSIX attached, then commits and pushes the
   next snapshot version. The tag therefore identifies source files set to the
   published version, while the branch ends at the next snapshot.

For the current release this produces version `1.0.3`, followed by
`1.0.4-SNAPSHOT` on the release branch.

## Outputs

- Maven Central publication of the parent POM, preview, Thymeleaf and JTE adapters, test-docs, and HTML catalog Java artifacts.
- Marketplace publication of `RebelCraft.java-html-toolkit`.
- GitHub Release tagged with the release version and containing the packaged VSIX.

## Notes

- Maven Central and the Marketplace do not provide a shared transaction, and
  published versions are immutable. If a run fails during either publication,
  inspect both registries before retrying. Maven Central is configured to ignore
  components that were already published and to wait until deployments are
  published; the Marketplace may reject a repeat publication of a version it
  already accepted. Resolve which channel still needs publication rather than
  blindly rerunning or trying to roll back a published artifact.
- The release-version commit is pushed only after both registries accept the
  release. If that push fails, or a later release/tag/snapshot step fails, inspect
  the selected branch and GitHub tag before taking action. Complete only the
  missing step: if the release commit was pushed, the tag must target that commit;
  if the tag exists, finish the next-snapshot update without republishing. Do not
  rerun the full workflow after a publication or tag may already exist.
- The workflow does not run automatically on tags; publication requires an explicit
  manual dispatch from a branch.
- The README's dependency example uses the latest intended release version, not the
  reactor's development snapshot.
