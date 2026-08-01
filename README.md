# Nekara Plugins

This repository contains Minecraft server plugins for the Nekara ecosystem.

## Projects

| Plugin | Status | Description |
| --- | --- | --- |
| `NekaraRPG` | active | Central modular RPG and immersion plugin. The first module is the migrated fishing minigame from NekaraFishing. |

## Current Direction

`NekaraRPG` is intended to become the central plugin for Nekara RPG systems.
Each gameplay area should be implemented as a module that can be enabled or
disabled in configuration. The first production module is `fishing`; no other
modules are active yet.

Possible future modules:

- `lockpicking`
- `wounds`
- `campfire`
- `world-events`
- `rumors`
- `territory`
- `reputation`

The plugin should avoid duplicating systems already handled well by ValhallaMMO.
ValhallaMMO integrations should remain optional and should preserve vanilla and
ValhallaMMO reward behavior rather than replacing it.

## Build NekaraRPG

```text
cd NekaraRPG
gradlew.bat clean test build
```

The verified JAR is produced at:

```text
NekaraRPG/dist/NekaraRPG-1.0.0.jar
```
