# Changelog

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
