# ADR 0014: Quality sockets and tiered hammering

## Status

Accepted.

## Decision

Crafted equipment quality is the sole source of rune-socket capacity. Common and
Uncommon equipment has one socket, Rare and Epic equipment has two, and Legendary
equipment has three. Socket entries are stored as a repeatable PDC list on the item.
Legacy single-rune keys remain readable and migrate when another rune is embedded.

An awakened rune is embedded through the vanilla anvil result, never by an
unprotected inventory gesture. The first production rune is Insight, created with
white dye. Its tiers add 1, 3 or 5 percent Skills experience. Multiple Insight runes
stack additively across both held items and equipped armor, then form one multiplier
in the central non-synthetic Skills XP pipeline.

Hammering a heated workpiece requires the workpiece in the off hand and a tagged
custom hammer in the main hand. The hammer material must be equal to or above the
workpiece material in the Smithing progression. At start, the server removes the
workpiece from the off hand into a per-player escrow so vanilla sneak interaction
cannot equip armor. The process continuously validates the escrowed workpiece,
hammer, sneaking and distance, and returns the workpiece on success, interruption,
disconnect or plugin shutdown. A tagged custom hammer is immediately usable after
crafting while retaining its quality and sockets. It does not enter its own heated-
workpiece pipeline, avoiding an impossible bootstrap for the first metal hammer.

## Consequences

Lore visualizes empty sockets as `◇` and occupied sockets as `◆`, followed by
the exact rune effect. Lore remains presentation only; PDC quality and socket data
are authoritative.

Old experimental rune effects can continue to resolve on existing items, but only
white dye maps to a craftable rune offer. Synthetic/admin XP does not gain a rune
multiplier.
