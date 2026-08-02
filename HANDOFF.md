# Nekara Plugins Handoff

## Current State

The repository is published at
[`Jonaczech/nekara-plugins`](https://github.com/Jonaczech/nekara-plugins).
The default branch is `main`.

The latest shipped plugin is **NekaraRPG 1.2.0**:

- tag: `v1.2.0`
- GitHub release: <https://github.com/Jonaczech/nekara-plugins/releases/tag/v1.2.0>
- deployable asset: `NekaraRPG.jar`
- source PR: <https://github.com/Jonaczech/nekara-plugins/pull/5>
- release merge commit: `44401180f12eee80d3246aa3cc1f9df95f79759d`
- release JAR SHA-256:
  `0D827BF58B7064BC3DA7AF37ED4F4D99FC76B0EFA760398EBC818548EB1D9691`
- release asset size: `181561` bytes
- automated validation: 35 passing tests

Release 1.2.0 moves reduced Rested hunger loss behind the smoker camp feature,
adds a configurable 10% Rested XP bonus across all ValhallaMMO skills,
introduces the optional Echo Vein Mining activity, and adds the asynchronous,
fail-closed GitHub updater. GitHub's latest-release endpoint, asset name, size,
SHA-256 digest, stable status, and tag target were verified after publication.
Live Purpur/ValhallaMMO gameplay and updater restart consumption remain pending
until 1.2.0 is installed manually on the staging server.

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

### Echo Vein

- Requires a real ValhallaMMO Mining XP action and Mining level 50 by default.
- Uses a rare six-second spatial pulse with an eight-minute persistent cooldown.
- Success grants 25% of the final source Mining XP with reason `PLUGIN`.
- The optional bonus is one item from that action's finalized Mining drops; no
  Digging table, custom loot table, or Fortune reroll is used.
- `/nekararpg test vein` exercises the visuals without XP, drops, or cooldown.

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

Starting with 1.2.0, later releases can be downloaded automatically from the
latest stable GitHub release into Paper's configured update folder. The updater
checks the exact asset name, size, SHA-256, JAR identity, and semantic version.
It never restarts the server; operators must perform a full restart and inspect
startup logs. Version 1.2.0 itself still needs one manual deployment because
1.1.0 does not contain the updater.

## Next Session

After live acceptance of 1.2.0, the next approved design direction is a
`mounts` module. Its first version should use a vanilla horse, one persistent
mount per player, no cargo inventory, and no hoglin model. Preserve saddle,
armor, name, health, ownership, and death cooldown while preventing summon,
dismiss, logout, and healing abuse in PvP.

Potential later Campfire extensions after the 1.2.0 candidate:

- unique gameplay bonuses for smoker, barrel, cauldron, cartography table, and
  grindstone,
- richer camp-quality progression based on structures around the fire.

Before starting new code, run `git status`, fetch `origin/main`, and re-check the
latest GitHub release so this snapshot is not mistaken for live state.
