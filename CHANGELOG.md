# Changelog

## 1.0.0

- Added the production `DEFERRED_CATCH` timing minigame for Purpur 26.1.2.
- A bite creates a pending gate; the first reel click creates the real catch event, whose original item is deferred until minigame success.
- Added configurable time recovery after successful timing hits (`minigame.time-bonus-ticks`, default 20 ticks).
- Preserved the original caught ItemStack and vanilla XP value without synthetic fishing events or custom loot tables.
- Preserved vanilla loot, XP, downstream fishing events, and avoided synthetic fishing events.
- Added configurable action-bar rendering, sounds, Czech messages, permissions, commands, validation, cleanup, and unit tests.
- Added manual Purpur acceptance scenarios.
- Added an optional reflection-based ValhallaMMO fishing XP bridge that defers profession XP until the original catch is delivered.
- Prevented the ValhallaMMO XP replay from being captured and cancelled by NekaraFishing itself.
- Preserved ValhallaMMO prepared extra fishing drops when its double-loot abilities are active.
- Limited post-hit target relocation distance and refreshed the action-bar renderer with colored fish-bar status indicators.
- Added a configurable per-player particle ring around the active fishing bobber.
- Removed the redundant `FISH` action-bar label and improved the compact bar contrast for the indicator, target, hit counter, and timer.
- Added configurable success and failure particle effects around the bobber. Effects are visual only and do not alter loot or XP.
- Shortened the default logical bar to 20 positions and slowed indicator movement to one step every 3 ticks.
- Cancelled additional bites during an active session and preserved the original deferred catch if an unexpected second catch event arrives.
- Temporarily suppressed further vanilla hook bites through the public Paper `FishHook` API while the deferred-catch minigame is active.
- Reworked the HUD into a timing action bar plus a segmented BossBar hit-progress indicator.
- Replaced the timing track with a wider line style and randomized required successful pulls between 3 and 5 by default.
- Refined the timing bar with `┠ ┨` boundaries, a hyphen track, highlighted `▰` target cells, and an `⟪⚓⟫` indicator.
- Added a configurable block-aware pull of the real bobber toward the player after each successful timing hit.
- Added optional-by-plugin, default-enabled ValhallaMMO FishingSkill difficulty scaling: higher fishing levels reduce required pulls and increase allowed misses.
- Replaced linear ValhallaMMO difficulty interpolation with configurable level tiers (1–30, 31–60, 61+) and an exact max-level override.
- Slowed the default timing indicator to one step every 6 ticks, increased the initial timeout to 160 ticks, and increased the per-hit time bonus to 30 ticks.
- Improved ValhallaMMO level detection by checking merged, skill, and persistent FishingSkill profiles so tier scaling does not silently fall back to level 0.
- Simplified the timing action bar to a single thick-line track with a bold colored target zone and a plain anchor indicator.
