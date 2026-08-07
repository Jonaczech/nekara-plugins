# ADR 0005: Derived power milestones gate shared rewards

## Status

Accepted

## Decision

Power remains a derived value with no direct experience source and no separate
spendable points. It continues to supply the shared perk-point pool. The first
level of any active skill produces Power 1; subsequent Power levels require a
further full set of active-skill levels. This preserves a broad-progression
requirement while making the initial onboarding milestone available promptly.

Milestone nodes are automatic, read-only, and derived from current Power. They
are never purchased or persisted separately. `campfire_rested` at Power 1 gates
only Rested campfire processing; sitting stays available. `mount` at
Power 25 gates player-facing NekaraMounts actions. Administrative mount grants
may preregister a mount, but player-facing use remains gated by the milestone.
`hero_aura` at Power 200 grants an original white-and-orange particle aura. It is
prestige-only and adds no combat, economy, movement, or progression modifier.

## Consequences

Milestone authority is evaluated in the Skills module from the cached profile.
Campfire and Mounts do not store duplicate unlock state. At Power 25 the player
has earned at least 313 aggregate active-skill levels, unless higher Power is
contributed by New Game Plus. Power 200 requires every active skill at level 100
after its New Game Plus reset; the aura is evaluated from that same derived state
and therefore requires no separate claim or persisted flag.
