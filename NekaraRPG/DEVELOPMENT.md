# NekaraRPG Development and Release

## Release Contract

Every shipped change ends as one version of the single `NekaraRPG` plugin JAR.
Modules are not built as separate plugins. A release is complete only when:

1. `gradle.properties` contains the intended `plugin_version`.
2. `CHANGELOG.md` contains a matching `## <version>` heading.
3. `scripts\build-release.cmd` completes successfully.
4. All unit tests pass before release artifacts are copied.
5. The version embedded in `plugin.yml` matches `plugin_version`.
6. The only deployable artifact is `dist\NekaraRPG.jar`; the version is stored
   inside its `plugin.yml` metadata rather than its filename.
7. The JAR passes the relevant checks in `TESTING.md` on a Purpur staging server.

Use a patch version for fixes and a minor version for a new module or meaningful
gameplay behavior. Keep fishing, sitting, and campfire independently toggleable
under `modules` in `config.yml`.

## Standard Workflow

From the `NekaraRPG` directory:

```powershell
scripts\build-release.cmd
```

The script performs a clean Gradle `release`. The Gradle task runs `build`, which
includes compilation and all tests, before copying any release artifact. It then
checks the embedded plugin version and prints the SHA-256 hash.

If local antivirus or a proxy replaces HTTPS certificates, Java may report a
`PKIX path building failed` error while resolving dependencies. Import the local
trusted root into a temporary Java truststore and pass it without committing it:

```powershell
scripts\build-release.cmd -JavaTrustStore C:\path\to\truststore.p12
```

Never disable Gradle TLS verification and never commit a machine-specific root
certificate or truststore.

## Module Shape

Each module implements `NekaraModule`, owns its listeners and scheduler tasks,
and must clean all player state and entities in `disable()`. Register modules in
dependency order in `NekaraRPGPlugin`: `fishing`, `sitting`, then `campfire`.

Configuration belongs in a typed record under `configuration`, defaults belong
in `config.yml`, and player-facing text belongs in `messages.yml`. Add pure unit
tests for timing, scaling, or state calculations whenever Bukkit itself is not
required.

## GitHub Handoff

Before publishing, inspect `git status` and the complete diff. Commit the source,
configuration, tests, changelog, and documentation together. Do not commit
`build/`, `dist/`, server files, Gradle caches, or truststores. The pull request
must state the version, modules changed, automated checks, and live-test status.
