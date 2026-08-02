# Changelog

## 1.2.5

- Made ore reveal height-aware. Overworld candidates now follow vanilla
  generation bands and relative height biases, so deep ores such as diamond
  and redstone cannot appear at high elevations.
- Added high-altitude iron, Y 0-96 copper, below-Y-32 gold, below-Y-64 lapis,
  below-Y-16 diamond/redstone, coal above Y 0, and elevated Badlands gold rules.
  Exact worldgen seed noise and air-exposure suppression are intentionally not
  reproduced.
- Limited netherrack reveals to the normal Y 10-117 Nether ore band. End stone
  still has no artificial ore reveal.
- Added a dedicated ore-reveal sound using a brighter amethyst cluster tone.
  The existing lower amethyst chime remains the distinct vein-discovery sound.
- Added pure tests for height boundaries, deep-ore bias, Badlands gold, Nether
  bounds, and weighted selection.

## 1.2.4

- Raised the default Echo Vein trigger chance from 4% to 5% and removed its
  per-player cooldown. Existing 4% defaults migrate to 5%; legacy
  `cooldown-seconds` values and stored cooldown timestamps are ignored.
- Limited automatic triggers and targets to stone, deepslate, netherrack, and
  end stone so ordinary excavation drives the activity while natural ores stay
  distinct.
- Changed natural completion from clicking the marker to actually mining the
  marked block. Its finalized ValhallaMMO Mining XP now supplies the one-time
  25% `PLUGIN` bonus and its own finalized drops supply the optional bonus item.
- Added a 50% chance for a completed target to continue into a visible
  face-adjacent host block. A chained target replaces the normal success sound
  with the next discovery sound and cannot also roll the independent 5% start.
- Added a one-time 25% ore reveal when mining of a marked block begins. Stone
  and deepslate use weighted Overworld ores, netherrack uses quartz or Nether
  gold, and end stone remains unchanged because vanilla has no native End ore.
- Added debug audit lines for triggering XP, marked-block XP, granted bonus XP,
  bonus drops, ore reveals, and chain results.

## 1.2.3

- Renamed the gameplay module from `echo-vein` to `mining` (`NekaraMining`),
  keeping Echo Vein as its first activity and migrating the previous module
  toggle when `modules.mining.enabled` is not yet present.
- Kept an active Echo Vein running when the player hits or mines another block;
  only striking the marked target completes it, while timeout and existing
  lifecycle cleanup rules still apply.
- Removed all natural Echo Vein chat messages. The action-bar timer remains,
  while command tests retain their explicit administrative feedback.
- Limited the discovery sound to one playback when the vein appears instead of
  repeating it with every particle pulse. Natural success plays one completion
  sound, and natural timeout is silent.

## 1.2.2

- Made Echo Vein available at every ValhallaMMO Mining level; the legacy
  `minimum-mining-level` setting is no longer used.
- Limited target selection to blocks in Paper's `MINEABLE_PICKAXE` tag so dirt,
  gravel, wood, and other non-pickaxe cave blocks cannot become the fissure.
- Added a layered pulse with a subtle one-block-area hint and a denser effect on
  the visible face of the target block.
- Reduced natural Echo Vein chat to one discovery message with the remaining
  time. Successes and failures now use only their sounds and visual feedback.
- Kept `/nekararpg test vein` reward-free; bonus XP and the one-item finalized
  drop reward remain exclusive to naturally triggered Echo Veins.

## 1.2.1

- Published the complete Echo Vein Mining activity in the first patch release
  intended for end-to-end updater acceptance from a manually installed 1.2.0.
- Kept the Mining level 50 trigger, ValhallaMMO XP bonus, finalized-drop bonus,
  persistent cooldown, and reward-free `/nekararpg test vein` behavior unchanged.
- No gameplay or configuration defaults changed from 1.2.0.

## 1.2.0

- Added the optional `echo-vein` module: Mining level 50 players can rarely
  reveal a nearby pulsing block and strike it with a pickaxe within six seconds.
- Added a configurable eight-minute persistent cooldown, 4% trigger chance,
  search radius, pulse timing, particles, and `/nekararpg test vein`.
- Added a 25% success bonus based on the final ValhallaMMO Mining XP amount,
  awarded with the `PLUGIN` reason so Rested and global multipliers do not stack
  a second time.
- Added one quantity-weighted bonus item from the triggering action's actual
  finalized natural and Valhalla-prepared Mining drops, capped to one item with
  its metadata preserved. No Digging treasure table or synthetic loot is used.
- Added a GitHub-backed updater for the latest stable `Jonaczech/nekara-plugins`
  release, with automatic checks and `/nekararpg update check|status`.
- Added strict release validation for the stable `NekaraRPG.jar` asset: trusted
  GitHub URL, size limit, SHA-256 digest, JAR descriptor, product identity, and
  semantic version must all match before staging.
- Staged verified updates in Paper's configured update folder for installation
  on the next full server restart; the active JAR is never replaced at runtime.
- Added a hash-verified backup of the running JAR before staging so operators
  retain a manual rollback artifact.
- Added configurable update intervals, automatic download, administrator join
  notices, timeouts, permissions, and fail-safe status reporting.
- Added a configurable 10% Rested XP bonus for every ValhallaMMO skill.
- Limited the XP bonus to normal skill actions and shared XP so commands,
  resets, redemptions, and migration refunds keep their exact values.
- Kept deferred ValhallaMMO fishing XP compatible with Rested without applying
  the bonus a second time during successful catch delivery.
- Made reduced Rested hunger loss depend on a smoker in the charged camp.
  Bare Rested no longer slows hunger depletion.
- Preserved fail-soft startup and normal skill XP when ValhallaMMO is absent or
  exposes an incompatible event API.

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
