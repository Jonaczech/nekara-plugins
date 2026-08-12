# ADR 0011: Readable vanilla enchanting before runes

## Status

Accepted.

## Context

NekaraRPG keeps the vanilla enchanting table as the familiar base progression,
but its default item tooltip does not explain numerical effects clearly. The
former Runotepectví perks could also promote an enchantment above its vanilla
maximum, which would stack poorly with the planned rune system.

## Decision

- The enchanting table keeps vanilla offer generation, compatibility, lapis,
  bookshelves and direct application to equipment.
- After a successful table enchant, NekaraRPG appends a replaceable `Nekara ·
  Výklad očarování` lore section. It describes the applied vanilla enchantments
  numerically where their effect has a stable player-facing value.
- Runotepectví never raises a vanilla enchantment above its vanilla maximum.
  Its current benefits are transparent XP-cost reduction and a chance to
  restore lapis after enchanting.
- Enchanted books and runes are not changed in this increment. The future rune
  station remains a separate system and must not reuse or alter vanilla offer
  generation.

## Consequences

Players can compare real enchantment results directly in an item tooltip while
vanilla balance remains intact. The later rune system can focus on conditional
and utility effects rather than duplicate Sharpness, Protection, Efficiency or
other vanilla enchantments.
