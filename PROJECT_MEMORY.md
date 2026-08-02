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
- Smoker controls reduced hunger loss after leaving the camp; bare Rested does
  not slow hunger depletion.
- Rested grants configurable bonus XP to every ValhallaMMO skill for normal
  skill actions and shared XP. Administrative and recovery XP is unchanged.
- Deferred fishing XP must receive the Rested multiplier exactly once.
- Bed controls natural hostile spawn protection in the wider camp radius.
- Group scaling affects healing and hunger restoration, with a configured cap;
  it does not multiply Rested duration or duplicate equipment bonuses.
- Rested uses action-bar text rather than a bossbar so MythicMobs boss health UI
  remains unmistakable. Fishing and charging feedback have higher priority.

## NekaraMining and Echo Vein Rules

- NekaraMining uses the module ID `mining`; Echo Vein is its first optional
  ValhallaMMO activity and is available at every skill level. A real
  non-cancelled Mining `SKILL_ACTION` XP event is required.
- Automatic triggers and targets are restricted to stone, deepslate,
  netherrack, and end stone in Paper's `MINEABLE_PICKAXE` block tag. Mining
  another block does not cancel the activity. Natural attempts use no chat,
  play one discovery sound and one final success sound, and keep timeout silent.
- The original mining action always completes before the challenge starts;
  failure never removes or changes its XP or drops.
- Mining the marked target grants a configurable fraction of that block's final
  Mining XP with reason `PLUGIN`, preventing Mining, Rested, and global
  multipliers from applying twice.
- The optional bonus drop is exactly one quantity-weighted item cloned from the
  finalized natural and Valhalla-prepared drops of the marked block. Echo
  Vein does not use the Digging treasure table or maintain custom loot.
- There is no cooldown. A completed target has a configurable chance to chain
  to a visible face-adjacent host block. The first target damage may reveal a
  weighted vanilla ore once. Ore candidates follow vanilla-compatible Y bands
  and relative height biases, including Badlands gold and the Nether Y 10-117
  band; seed noise and air exposure are not reproduced. End stone stays
  unchanged. Vein discovery and successful ore reveal use distinct related
  sounds. Fishing has interaction priority over an active echo.
- If `modules.mining.enabled` is absent, preserve the legacy
  `modules.echo-vein.enabled` value. Keep activity settings under `echo-vein`.

## Mounts Direction

- A future `mounts` module should begin with a vanilla horse, one mount per
  player, no cargo inventory, and no hoglin model.
- Saddle, armor, name, health, death cooldown, and ownership need persistence.
  Summoning and dismissal must not become a PvP escape or healing exploit.

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
- NekaraRPG 1.2.0 and later may stage a verified stable GitHub release in
  Paper's configured update folder. Installation still occurs only on a full
  restart; the plugin never replaces its active JAR or restarts the server.
- The updater trusts only `Jonaczech/nekara-plugins`, the exact stable
  `NekaraRPG.jar` asset, GitHub SHA-256 metadata, and matching JAR identity and
  semantic version. Network work must remain asynchronous and fail closed.

## Collaboration Preferences

- Work and communicate with the user in Czech.
- Implement and verify requested changes end to end instead of stopping at a
  proposal unless the user explicitly asks only for ideas or planning.
- Keep documentation and handoff state current before moving to another module
  or plugin.
