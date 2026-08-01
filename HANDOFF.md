# Nekara Plugins Handoff

## Current State

The repository is published at
[`Jonaczech/nekara-plugins`](https://github.com/Jonaczech/nekara-plugins).
The default branch is `main`.

The latest shipped plugin is **NekaraRPG 1.1.0**:

- tag: `v1.1.0`
- GitHub release: <https://github.com/Jonaczech/nekara-plugins/releases/tag/v1.1.0>
- deployable asset: `NekaraRPG.jar`
- release merge commit: `ce594898c049ca7637443dcc3af4fcc628c4a0f7`
- release JAR SHA-256:
  `63ED962AE9AE94F3D09ECCDD2B9858E5C51A005FD0E7963D72AB66A2F093947D`
- automated validation: 22 passing tests

Version numbers belong in plugin metadata, changelogs, and Git tags. The JAR
filename intentionally stays stable so server deployment never creates a
second versioned copy beside the active plugin.

## Shipped NekaraRPG Modules

### Fishing

- Uses deferred catch handling while preserving the original vanilla item and XP.
- Preserves optional ValhallaMMO loot, profession XP, and difficulty behavior.
- Uses action-bar timing and its own minigame bossbar.

### Sitting

- Commands: `/nekararpg sit`, `/nrpg sit`, and `/nekararpg stand`.
- Does not register top-level `/sit`; CMI remains the owner of that command.
- Detects configured external seats, with `ARMOR_STAND` as the default for CMI.
- Current server-side seat Y offset is `0.20`.

### Campfire

- Accepts NekaraRPG or supported external seating within five blocks of a lit
  campfire or soul campfire.
- Heals slowly, blocks hunger loss, restores a small amount of hunger, and uses
  a capped per-fire group multiplier.
- Grants Rested after 20 real-time seconds. The base duration is five minutes.
- Every unique camping feature type adds one minute: crafting table, bed,
  smoker, barrel, water cauldron, cartography table, and grindstone.
- Duplicate feature blocks do not stack. All seven defaults produce a maximum
  Rested duration of 12 minutes.
- A crafting table adds managed Haste I. A bed creates a 24-block safe-camp
  radius against natural hostile spawns.
- Optional MythicMobs support blocks natural `NekaraHostile` random spawns but
  leaves `NekaraFauna`, existing mobs, and scripted encounters untouched.
- Rested uses text-only action-bar UI: `Odpočatý | m:ss`. It yields to campfire
  charging messages and the fishing minigame. It does not create a bossbar.

## Build and Deployment

From `NekaraRPG` run:

```powershell
scripts\build-release.cmd
```

The script builds and verifies the single artifact at
`NekaraRPG/dist/NekaraRPG.jar`. If local TLS inspection causes a PKIX error,
pass a machine-local PKCS12 truststore through `-JavaTrustStore`; never commit
the truststore or disable TLS verification.

For server deployment:

1. Stop the server.
2. Confirm there is no second `NekaraRPG*.jar` or legacy `NekaraFishing*.jar`.
3. Replace only `plugins/NekaraRPG.jar`.
4. Start the server and inspect startup logs.
5. Use `/nekararpg reload` only for configuration/messages, never to replace a JAR.

Do not delete `plugins/NekaraRPG` during routine upgrades. Missing new keys use
bundled runtime defaults.

## Next Session

The user wants to design another plugin. Its purpose and ownership boundary are
not selected yet. Start by deciding:

1. What player or staff problem does it solve?
2. Does it need direct access to Rested, fishing, sitting, or other NekaraRPG
   state? If yes, prefer a new NekaraRPG module.
3. Does it have an independent lifecycle, data model, commands, permissions, or
   administrator audience? If yes, prefer a separate plugin JAR.
4. Which existing server plugins already own adjacent behavior?

Candidate ideas already recorded are lockpicking, wounds, world events, rumors,
territory ambience, and reputation. No candidate is approved as the next build.

Potential later Campfire extensions, not part of 1.1.0:

- ValhallaMMO XP bonuses while Rested,
- unique gameplay bonuses for smoker, barrel, cauldron, cartography table, and
  grindstone,
- richer camp-quality progression based on structures around the fire.

Before starting new code, run `git status`, fetch `origin/main`, and re-check the
latest GitHub release so this snapshot is not mistaken for live state.
