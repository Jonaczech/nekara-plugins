# ADR 0015: Ranked Runecrafting perk branches

## Status

Accepted.

## Context

The original Runecrafting tree mixed tier unlocks, vanilla enchanting economy and
small generic statistics. Several nodes had only one rank, Tier II and III checked
perk ownership instead of the intended rank, and the experience-orb statistic was
not connected to global orb collection.

## Decision

Runecrafting retains its six stable perk IDs but uses a skill-specific ranked tree:

- the left branch controls rune XP cost, dye preservation and Tier II/III access;
- the right branch controls lapis preservation and experience gains;
- Rune Memory requires rank III of both deep branch perks.

Perk definitions may use `RankedStatPerkEffect` when cumulative values are not
linear. Tier access is server-authoritative and checks exact cached ranks. New
Game+ strengthens all statistical values through the existing central resolver;
Rune Memory keeps its explicit 10 to 20 percent New Game+ rule.

## Consequences

Existing perk IDs and their original point costs remain stable. Existing stored
ranks stay valid because no maximum rank was reduced. The rune GUI derives prices
and preservation chances from resolved statistics, and collected experience orbs
now apply the Runecrafting orb multiplier with the shared fractional accumulator.