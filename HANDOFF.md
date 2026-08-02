# Nekara Plugins Handoff

## Current State

The repository is published at
[`Jonaczech/nekara-plugins`](https://github.com/Jonaczech/nekara-plugins).
The default branch is `main`.

The latest shipped plugin is **NekaraRPG 1.2.5**:

- tag: `v1.2.5`
- GitHub release: <https://github.com/Jonaczech/nekara-plugins/releases/tag/v1.2.5>
- deployable asset: `NekaraRPG.jar`
- source PR: <https://github.com/Jonaczech/nekara-plugins/pull/15>
- release merge commit: `47944ab70e7a88a07376a9945449ea5470c80e73`
- release JAR SHA-256:
  `D6D95DBE342DF9F9161EA154DC1709286BC50611C3B8C5B8AEF7166AD0A4AFA2`
- release asset size: `190588` bytes
- automated validation: 42 passing tests

Release 1.2.5 keeps the 1.2.4 Echo Vein flow and makes ore reveal
height-aware. Candidates follow vanilla-compatible Y bands and relative height
biases, including high Badlands gold and the Nether Y 10-117 range. Diamond and
redstone cannot appear high above their normal region. Seed noise and vanilla
air-exposure suppression are intentionally not reproduced. Successful ore
transformation now uses a brighter amethyst sound distinct from the lower vein
discovery chime. GitHub release metadata, downloaded JAR hash, stable status,
single asset, and tag target were verified. Live Purpur/ValhallaMMO sound and
balance acceptance remains pending.

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

### NekaraMining / Echo Vein

- Uses module ID `mining`; legacy `modules.echo-vein.enabled` is the fallback
  until `modules.mining.enabled` is explicitly present.
- Requires a real ValhallaMMO Mining XP action but has no level gate.
- Triggers from and selects only stone, deepslate, netherrack, and end stone in
  Paper's `MINEABLE_PICKAXE` tag. There is no cooldown.
- Marks the visible target face more strongly than the surrounding one-block
  area and requires the target to be actually mined for natural completion.
- Success grants 25% of the marked block's final Mining XP with reason `PLUGIN`.
- The optional bonus is one item from that marked block's finalized Mining
  drops; no Digging table, custom loot table, or Fortune reroll is used.
- Completion has a 50% chance to continue into a visible face-adjacent host.
- First target damage rolls a one-time 25% weighted ore reveal. Stone and
  deepslate use height-compatible Overworld ores, netherrack uses quartz or
  gold only from Y 10 through 117, and end stone remains unchanged. Badlands
  can reveal elevated gold.
- Striking or mining another block does not cancel the active target.
- Natural attempts use no chat, one discovery sound, one success sound, and a
  silent timeout. Successful ore transformation adds its own brighter related
  sound. The action bar still carries the timer.
- `/nekararpg test vein` exercises the visuals without XP, drops, ore reveal,
  or chaining.

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

After live acceptance of 1.2.3, the next approved design direction is a
`mounts` module. Its first version should use a vanilla horse, one persistent
mount per player, no cargo inventory, and no hoglin model. Preserve saddle,
armor, name, health, ownership, and death cooldown while preventing summon,
dismiss, logout, and healing abuse in PvP.

Potential later Campfire extensions after the 1.2.3 acceptance pass:

- unique gameplay bonuses for smoker, barrel, cauldron, cartography table, and
  grindstone,
- richer camp-quality progression based on structures around the fire.

Before starting new code, run `git status`, fetch `origin/main`, and re-check the
latest GitHub release so this snapshot is not mistaken for live state.
