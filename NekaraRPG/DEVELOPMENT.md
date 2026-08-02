# NekaraRPG Development and Release

## Release Contract

Every shipped change ends as one version of the single `NekaraRPG` plugin JAR.
Modules are not built as separate plugins. A release is complete only when:

1. `gradle.properties` contains the intended `plugin_version`.
2. `CHANGELOG.md` contains a matching `## <version>` heading.
3. `scripts\build-release.cmd` completes successfully.
4. All unit tests pass before release artifacts are copied.
5. The version embedded in `plugin.yml` and the JAR implementation manifest
   matches `plugin_version`.
6. The only deployable artifact is `dist\NekaraRPG.jar`; the version is stored
   inside its `plugin.yml` metadata rather than its filename.
7. The JAR passes the relevant checks in `TESTING.md` on a Purpur staging server.
8. The published GitHub release is stable, tagged `v<version>`, contains exactly
   one `NekaraRPG.jar`, and exposes its SHA-256 digest through release metadata.

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

## Echo Vein Contract

Echo Vein is a separate optional module and must remain fail-soft when
ValhallaMMO or its Mining skill is unavailable. A Bukkit block break alone is
not eligibility evidence: the module correlates the break with a non-cancelled
ValhallaMMO Mining `SKILL_ACTION` XP event.

The source XP amount is observed after other XP modifiers. A delayed success
uses Valhalla's `PLUGIN` reason so global and Rested multipliers are not applied
again. Bonus loot is selected from cloned final natural drops and Valhalla's
prepared block drops for the same action. Selection is weighted by actual stack
amount, then capped to one item; never recalculate Fortune or generate a
separate loot table.

Echo Vein is available at every Mining level. Target blocks must belong to
Paper's `MINEABLE_PICKAXE` tag. Natural attempts send only their initial timed
discovery message; completion feedback remains visual and audible without chat.

Challenge cooldown timestamps use the player's persistent data container.
Transient pending breaks and active challenges stay in memory and must be
cleared on disable, reload, disconnect, or invalid state.

## Updater Contract

The updater is a core service because it owns the lifecycle of the complete JAR,
not one gameplay module. GitHub checks and downloads run asynchronously. Only
the fixed public repository, latest stable release endpoint, stable asset name,
and HTTPS GitHub download path are trusted.

The downloaded artifact is first written to a unique temporary file with a
configured size limit. It must match GitHub's declared size and SHA-256 digest,
contain `plugin.yml`, and identify itself as the release version of NekaraRPG in
its JAR manifest. Only then is it moved into Paper's configured update folder.
Before staging, the running JAR is copied into the plugin data folder and its
SHA-256 is checked so an operator has a manual rollback artifact. The service
must never overwrite the active JAR, invoke Bukkit reload, or restart the server.

## GitHub Handoff

Before publishing, inspect `git status` and the complete diff. Commit the source,
configuration, tests, changelog, and documentation together. Do not commit
`build/`, `dist/`, server files, Gradle caches, or truststores. The pull request
must state the version, modules changed, automated checks, and live-test status.
