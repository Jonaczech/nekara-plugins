# ADR 0007: Gathering level rewards and New Game+ scaling

## Context

The gathering skills need a predictable baseline reward for every level while
perk trees remain a meaningful source of specialization.  New Game+ must slow
progress without making a completed skill feel weaker.

## Decision

Mining, Woodcutting, Digging, Farming and Fishing receive an innate `0.20 %`
double-drop chance per skill level, capped at `20 %` at level 100.

The bonus applies only to the following sources:

- Mining: configured pickaxe-mined blocks, including stone and ores.
- Woodcutting: logs and stems only; leaves retain their separate rare-find
  mechanics.
- Digging: configured shovel-mined blocks.
- Farming: mature crops, melons, pumpkins, mushrooms and flowers.
- Fishing: one additional copy of the actual vanilla catch only. Treasure and
  Luck-driven loot are not copied.

Each of those perk trees has one `+5 %` double-drop perk. Mining, Woodcutting,
Digging and Farming also retain one `+3 %` triple-drop perk. The Fishing tree
has no triple-catch perk.

New Game+ uses an experience multiplier of `0.75` per rank. Every rank raises
all purchased perk statistics by `10 %` and multiplies the innate gathering
double-drop chance by `1.25`. At level 100, the first rank therefore changes
the innate chance from `20 %` to `25 %`; a `+5 %` double-drop perk becomes
`+5.5 %` through the perk-stat multiplier.

## Consequences

The baseline reward is controlled centrally in `skills/config.yml`, and all
drop resolution stays server-authoritative. Fishing keeps vanilla catch and
treasure handling separate, preventing Luck or treasure mechanics from being
duplicated by the gathering reward.
