# ADR 0012: Rune workstation progression

## Status

Accepted.

## Decision

Runotepectví uses a four-station, deterministic progression:

1. A crafting table creates an inert Blank Rune from coal, smooth stone and
   redstone.
2. An anvil plus one amethyst shard activates it into a Magical Rune.
3. Right-clicking the enchanting table with a Magical Rune opens a server-owned
   27-slot selection inventory. It displays the stored rune, the player's white-dye
   count and Tier I–III buttons. A selected tier is validated server-side. Its
   base cost of 6 / 9 / 12 XP levels is reduced by Šetrný zápis; 1 / 2 / 3
   white dye is preserved by a successful Za hranou písma roll. No item is
   inserted into the native lapis-only slot.
4. A right-click with an Unstable Rune on an empty lectern immediately awakens it
   with audiovisual feedback. The former symbol-selection minigame is removed.
   An awakened rune is then engraved through a normal anvil result.
   Tier I requires level 1 plus rank I of Čitelné runy; Tier II requires level 30
   plus rank III of Šetrný zápis; Tier III requires level 70 plus rank III of
   both Šetrný zápis and Za hranou písma.

The custom selection inventory is necessary because the vanilla client rejects
non-lapis items from the enchanting secondary slot before Bukkit can treat white
dye as a valid ingredient. The runic interactions are enabled only for tagged rune items. Vanilla
enchanting, lecterns containing books, and unrelated anvil combinations remain
unchanged.

## Consequences

Vanilla enchantments remain the predictable base-performance layer while runes
add conditional effects through quality-owned sockets. Each socket stores an effect ID and tier in PDC. Lore and the visual glint are presentation only.

All rune stages use the stable `nekararpg:runes/placeholder` item model from the separate
resource pack. This presentation cannot affect server authority or PDC identity.
