# Changelog

## 1.1.0

- Added the `sitting` module with `/nekararpg sit` and `/nekararpg stand`.
- Added invisible, non-persistent armor-stand seats with cleanup on dismount,
  teleport, death, disconnect, damage, module disable, and plugin shutdown.
- Added the `campfire` module for seated players near lit campfires and soul campfires.
- Added slow healing, hunger-loss prevention, and small configurable hunger restoration.
- Added a real-time Rested bonus after 20 seconds, lasting five minutes by
  default, which reduces average hunger loss.
- Added per-campfire group multipliers for healing and hunger restoration.
- Added configurable Czech action-bar roleplay and progress messages.
- Made new bundled messages available as runtime defaults without overwriting an
  existing server-customized `messages.yml` during upgrade.
- Kept a charged Rested bonus refreshed while the player remains at the fire, so
  its full configured duration is available after standing up.
- Added module status counts, permissions, configuration, unit-tested rest math,
  a verified release workflow, and staged live-testing documentation.
- Kept `/sit` unregistered to avoid collisions with CMI and other sitting plugins.
- Fine-tuned the default NekaraRPG seat position to `0.20` and migrated all
  earlier pre-release offsets when loading an existing test configuration.
- Added configurable external seat detection, defaulting to `ARMOR_STAND`, so
  Campfire can recognize CMI-style seats without taking over CMI commands.
- Decoupled Campfire from the internal Sitting module when an external seat
  provider is used.
- Added configurable active-rest particles and optional Rested particles.
- Added a managed Haste effect with Minecraft's standard top-right status icon;
  stronger or unrelated existing Haste effects are left untouched.
- Added a compact repeating Rested action-bar timer that yields to charging
  roleplay and the fishing minigame.
- Added a configurable soft amethyst chime when the 20-second Rested charge
  completes.
- Added camp equipment quality: each unique configured block type within five
  blocks adds one minute to Rested, up to twelve minutes with all defaults.
- Made Haste I depend on a crafting table in the camp instead of granting it at
  every bare campfire.
- Styled the Rested timer as concise Czech text, `Odpočatý | m:ss`, without a
  bossbar that could be confused with MythicMobs health bars.
- Added an explicit bundled-message fallback for servers retaining an older
  `messages.yml`.
- Added bed-based safe camps with a configurable 24-block radius that block
  natural vanilla hostile spawns without removing existing mobs.
- Added optional MythicMobs 5.12.1 pre-spawn integration. It blocks only natural
  random spawns in the configured `NekaraHostile` faction and leaves fauna and
  scripted encounters untouched.
- Made the staging deploy script reject duplicate versioned or legacy Nekara
  plugin JARs before replacing the stable `NekaraRPG.jar`.
- Standardized release artifacts on the single stable filename `NekaraRPG.jar`;
  release versions now live in plugin metadata, the changelog, and Git tags.

## 1.0.1

- Fixed `/nekararpg test` so the standalone fishing minigame starts correctly.
- Initialized test sessions in `WAITING_FOR_BITE` before starting them; without
  this state transition, the test session was rejected by the active-minigame
  guard and the minigame never rendered.

## 1.0.0

- Renamed the plugin from NekaraFishing to NekaraRPG.
- Added a modular core with `modules.fishing.enabled` as the first default-enabled module.
- Added `/nekararpg` and `/nrpg`; kept `/nekarafishing` and `/nfishing` as legacy aliases.
- Added new `nekararpg.*` permissions while keeping legacy `nekarafishing.*` permissions accepted.
- Preserved the production `DEFERRED_CATCH` fishing minigame for Purpur 26.1.2.
- Preserved the original caught ItemStack, vanilla XP, ValhallaMMO profession XP replay, and ValhallaMMO prepared extra drops without synthetic fishing events or custom loot tables.
- Kept the current quiet chat behavior, action-bar timing UI, BossBar hit progress, bobber particles, success/failure effects, hook pull, timeout bonus, and ValhallaMMO difficulty tiers.
