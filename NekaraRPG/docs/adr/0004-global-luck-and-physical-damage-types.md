# ADR 0004: Global Luck and physical damage types

## Status

Accepted

## Context

Nekara needs one player-wide Luck value without allowing it to multiply ordinary gathering,
crafting, vanilla fishing, chest loot, or unrelated server loot. Combat also needs readable
physical categories so weapon choice and armor have a small tactical distinction while retaining
Minecraft's native armor calculation and existing Nekara dodge, critical, cleave, and stun effects.

## Decision

- `LUCK` is a global statistic. Its current sources are the two fishing-tree perks; it is summed
  across active skill trees and capped by `skills.luck.maximum-points`.
- Luck only adds the configured fixed chance bonus when Nekara itself rolls one of its rare-loot
  tables. It never changes crafting, ordinary block drops, vanilla chest loot, vanilla fishing
  catch selection, or equipment-fishing rewards.
- The existing rare tables are woodcutting, digging, and fishing. Mining and farming gain no
  artificial reward until they have their own rare-loot tables.
- Weapon categories are `SLASH`, `PIERCE`, and `IMPACT`: swords/axes/greatswords, daggers/spears/arrows,
  and hammers respectively. Custom Nekara weapon lore shows the category.
- Leather, chainmail, iron, diamond, and netherite armor add a small type-specific reduction on
  top of native armor. Plate is strongest against slash and pierce and intentionally weakest
  against impact. Partial sets receive a proportional bonus.
- Bleed is a distinct damage-over-time category. It does not receive Nekara type protection and
  its synthetic damage removes Bukkit's armor modifier while preserving other applicable reductions.
- Parry is not enabled. The existing perk id remains as the diamond light-weapon mobility node to
  preserve previously stored progression without retaining a blocking counterattack mechanic.

## Consequences

All balance knobs are server-side in `skills/config.yml`. The design intentionally avoids copying
external formulas, values, code, or configuration structures. Future mining/farming rare tables
automatically become Luck-aware when they use the shared rare-loot resolver.
