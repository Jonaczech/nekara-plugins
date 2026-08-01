# Repository Guidance

These instructions apply to the whole `nekara-plugins` repository.

## Start Here

Before changing code or planning a new plugin, read:

1. `HANDOFF.md` for the current release and immediate next steps.
2. `PROJECT_MEMORY.md` for durable product and compatibility decisions.
3. The target plugin's `README.md`, `CHANGELOG.md`, `DEVELOPMENT.md`, and
   `TESTING.md` when those files exist.

Re-check Git, GitHub, build output, and server state when current facts matter;
handoff snapshots can become stale.

## Communication

- Communicate with the user in Czech unless they request another language.
- Keep player-facing Minecraft messages in Czech.
- Do not silently turn a proposed independent plugin into a NekaraRPG module,
  or the reverse. Establish the ownership boundary first.

## Engineering

- Preserve existing plugin and server integrations unless a change is explicit.
- Keep modules independently configurable and clean up listeners, tasks,
  entities, UI, and player state when disabled or reloaded.
- Prefer typed configuration, bundled defaults, and focused tests for pure
  timing, scaling, deduplication, and state logic.
- Do not commit build output, server files, credentials, Gradle caches, local
  truststores, or anything under `dist/`.
- Never expose credentials from local server or FTP configuration files.

## NekaraRPG Releases

- Use semantic versions internally, in changelog headings, and in Git tags.
- The only deployable filename is `NekaraRPG.jar`; never add a version to it.
- Run `NekaraRPG\scripts\build-release.cmd` before publishing.
- Keep exactly one NekaraRPG JAR in a server's `plugins` directory, replace it
  only while the server is stopped, and restart instead of using Bukkit reload.
- Publish releases through a reviewed branch/PR, merge to `main`, then create a
  GitHub release whose only plugin asset is `NekaraRPG.jar`.
