# Nekara Plugins Project Memory

This file records durable decisions and rationale. Update it when a future
release deliberately changes one of these decisions.

## Product Shape

- `NekaraRPG` is the central plugin for connected player-facing RPG and
  immersion systems.
- A feature belongs in NekaraRPG when it shares its player state, lifecycle,
  configuration, commands, or integrations. A genuinely independent system may
  become a separate plugin in this repository.
- Modules must remain independently toggleable even though they ship in one JAR.
- Avoid rebuilding mature systems already owned by CMI, ValhallaMMO,
  MythicMobs, Lands, or another production plugin. Integrate narrowly instead.

## Compatibility Contracts

- Fishing must preserve the server-created catch, item metadata, vanilla XP,
  ValhallaMMO profession XP, and optional extra drops. Do not synthesize a
  replacement catch event or recalculate another plugin's rewards.
- CMI owns `/sit`. NekaraRPG exposes `/nekararpg sit` and recognizes external
  vehicle-based seats without a hard CMI dependency.
- MythicMobs is a soft dependency. Safe camps filter only natural hostile
  random spawns in faction `NekaraHostile`; fauna and scripted spawns remain.
- Existing hostile mobs are never deleted by safe camps and can walk into them.
- ValhallaMMO and MythicMobs integrations must fail softly when absent.

## Campfire and Rested Rules

- A player rests only while seated near the nearest lit campfire.
- Time uses wall-clock seconds so server lag does not lengthen the charge.
- Rested base duration is five minutes after a 20-second charge.
- Camping quality counts unique feature types, not block quantity. Each enabled
  type contributes once, currently one minute.
- Crafting table controls Haste I; Haste is managed without stacking or
  overwriting stronger external effects.
- Bed controls natural hostile spawn protection in the wider camp radius.
- Group scaling affects healing and hunger restoration, with a configured cap;
  it does not multiply Rested duration or duplicate equipment bonuses.
- Rested uses action-bar text rather than a bossbar so MythicMobs boss health UI
  remains unmistakable. Fishing and charging feedback have higher priority.

## Configuration and Upgrade Rules

- Bundled defaults must cover missing keys in existing `config.yml` and
  `messages.yml`; routine upgrades must not require deleting the plugin folder.
- Preserve user-customized settings and messages. Migrate only known
  pre-release defaults where compatibility requires it.
- Keep player-facing text configurable and provide a bundled fallback so raw
  message keys are never shown.
- Use typed configuration records and validate numeric limits and enum names.

## Release Rules

- Current server target: Purpur/Paper 26.1.2 on Java 25.
- The deployable plugin is always named `NekaraRPG.jar`.
- Never commit `build/`, `dist/`, credentials, server files, caches, or local
  certificate truststores.
- A release requires a matching semantic version and changelog heading, a clean
  release build, passing tests, embedded plugin-version validation, a Git tag,
  and a GitHub release.
- GitHub release assets contain only the stable plugin JAR unless another asset
  is explicitly approved.
- Server replacement happens while stopped, with exactly one NekaraRPG JAR in
  `plugins`; Bukkit `/reload` is not a deployment mechanism.

## Collaboration Preferences

- Work and communicate with the user in Czech.
- Implement and verify requested changes end to end instead of stopping at a
  proposal unless the user explicitly asks only for ideas or planning.
- Keep documentation and handoff state current before moving to another module
  or plugin.
