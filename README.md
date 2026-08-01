# Nekara Plugins

This repository contains Minecraft server plugins for the Nekara ecosystem.

## Projects

| Plugin | Status | Description |
| --- | --- | --- |
| `NekaraRPG` | active | Central modular RPG and immersion plugin with fishing, sitting, and campfire rest. |

## Current Direction

`NekaraRPG` is intended to become the central plugin for Nekara RPG systems.
Each gameplay area is implemented as a module that can be enabled or disabled
in configuration. The current release includes `fishing`, `sitting`, and
`campfire`; campfire uses sitting as its player-state foundation.

Possible future modules:

- `lockpicking`
- `wounds`
- `world-events`
- `rumors`
- `territory`
- `reputation`

The plugin should avoid duplicating systems already handled well by ValhallaMMO.
ValhallaMMO integrations should remain optional and should preserve vanilla and
ValhallaMMO reward behavior rather than replacing it.

## Release NekaraRPG

```text
cd NekaraRPG
scripts\build-release.cmd
```

The release script runs all tests, verifies the internal version and changelog,
and produces one stable deployment artifact:

```text
NekaraRPG/dist/NekaraRPG.jar
```

See `NekaraRPG/DEVELOPMENT.md` for the release contract and
`NekaraRPG/LIVE_TESTING.md` for server deployment and acceptance testing.
