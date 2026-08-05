# ADR 0001: Native Nekara skills platform

- Status: Accepted for implementation
- Target: NekaraRPG 2.0.0
- Date: 2026-08-03

## Context

NekaraRPG is the authoritative RPG progression system for skills, perk trees,
combat statistics and gathering bonuses. The target is sixteen player-facing skills with level 100 caps, a shared
Power level, data-driven perk graphs, custom item support and exploit-resistant
experience and reward processing.

All names, trees, values, text, layouts and implementation in NekaraRPG remain
original. External code and assets are never copied into this project.

## Decision

### Skill model

The player-facing catalog contains sixteen skills:

1. Power
2. Martial Arts
3. Trading
4. Smithing
5. Enchanting
6. Alchemy
7. Mining
8. Woodcutting
9. Digging
10. Hospodářství (`farming`)
11. Fishing
12. Light Weapons
13. Heavy Weapons
14. Archery
15. Light Armor
16. Heavy Armor

Fifteen gameplay skills gain experience directly and cap at level 100. Power is
derived from their combined levels and cannot receive direct experience. By
default, Power is the floor of the average gameplay-skill level. This prevents a
single repetitive activity from unlocking account-wide milestones. The formula
is encapsulated so a later weighted model does not require persistence changes.

Power grants the shared perk-point budget and provides milestone conditions for
other NekaraRPG modules. A mount reward at Power 25 uses a generic milestone
API; BetonQuest may own the actual story presentation and claim flow.

### Progression and persistence

- Experience is stored as integer units, never floating-point totals.
- Curves are deterministic, monotonic, validated and configurable per skill.
- SQLite is the authoritative store behind `SkillProfileRepository`.
- Writes use optimistic revisions and transactions. Conflicting writes fail
  closed and are retried from a fresh snapshot.
- Profile, experience ledger, perk ranks and milestone claims use separate
  tables so rewards can be idempotent and audited.
- Runtime reads use immutable snapshots. Bukkit events never hold a database
  transaction open.

### Perks

Perks form a validated directed acyclic graph. Each definition has a namespaced
ID, owning skill, maximum rank, skill-level requirement, point cost, prerequisite
ranks, viewport position and typed effects. Startup rejects unknown references,
cycles, impossible ranks and duplicate positions.

Effects are declarative:

- stat modifiers such as damage, critical chance, bleed, stun, gathering yield,
  mining speed or crop growth;
- mechanic unlocks such as Vein Mining, Tree Feller, Field Harvest, Drilling,
  Parry or Charged Shot;
- milestone hooks for modules and BetonQuest.

The first release will keep approximately the same tree density players expect
from mature skill plugins, but it will not copy another plugin's exact nodes,
names, values or graph.

### Statistics and combat pipeline

Statistics are calculated from uniquely identified sources. Flat modifiers are
summed, multiplicative modifiers are multiplied, and the final result is clamped
to the stat's declared bounds. Duplicate source IDs replace rather than stack.

Combat processing uses one guarded pipeline:

1. classify attack and equipment,
2. take one immutable attacker/defender stat snapshot,
3. calculate base and power-attack damage,
4. roll one deterministic critical decision,
5. calculate armor and penetration,
6. schedule typed secondary effects such as bleed or stun,
7. publish final damage and experience facts exactly once.

Secondary damage carries provenance and cannot recursively trigger criticals,
bleed, stun, lifesteal or experience unless explicitly allowed.

### Gathering and active abilities

Block rewards use captured original drops and server loot calculations. Bonus
drops clone eligible final drops and never call the original break pipeline
again. Double and triple yield are mutually exclusive outcomes from one roll.

Vein Mining, Tree Feller and Field Harvest have bounded searches, block-count
caps, chunk-load restrictions, durability costs, permission checks, cooldowns
and per-operation visited sets. They do not load chunks and cannot recurse.

### Anti-exploit contract

- Cancelled, Creative, Spectator and plugin-synthetic events grant no experience.
- Player-placed blocks are tracked independently of material and grant no
  gathering experience or bonus yield unless explicitly allowed.
- Event fingerprints make experience and rewards idempotent.
- Combat sources track entity identity, spawn provenance and recent activity per
  chunk. Repeated farm activity is reduced by configurable soft and hard limits.
- Rewards are committed before delivery where duplication is possible.
- Custom items use Persistent Data Container IDs and schema versions, not lore.
- Unknown or invalid item data fails closed without deleting the item.

### GUI

The skill GUI uses a 54-slot inventory viewport inspired by the supplied image:
a central perk graph, directional navigation and a persistent lower skill bar.
The renderer owns presentation only. Unlock validation stays in the domain
service, and every purchase uses a confirmation screen and optimistic revision.
Empty/filler clicks remain inside the current screen.

## Rollout

1. Domain kernel, curve, Power calculation, perk graph validation, stat engine
   and experience security primitives.
2. SQLite profiles and audit ledger with migration and recovery tests.
3. Read-only skill overview and navigable perk-tree GUI.
4. Perk purchasing, refund administration and milestone API.
5. Gathering skills and bounded active abilities.
6. Combat, armor and secondary-effect pipeline.
7. Crafting, alchemy, enchanting, custom items and Trading.
8. Controlled migration, staging soak and independent operation.

NekaraRPG 2.0.0 may publish the foundation for controlled testing only while the
native skills module remains disabled by default and does not award experience,
perks or rewards. Nekara Skills cannot become the default authority until each
skill has experience input, an original validated tree, documented effects,
exploit tests and live Purpur acceptance.

## Consequences

This is a large multi-release implementation, but it creates an explicit and
testable core instead of coupling unrelated listeners. Current Fishing,
Campfire, Mining and Mounts mechanics will migrate through stable internal APIs.
NekaraRPG operates independently after data migration and parity acceptance.
