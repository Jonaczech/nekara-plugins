# ADR 0002: Weapon catalog and combat families

- Status: Accepted for implementation
- Target: NekaraRPG 2.x
- Date: 2026-08-05

## Context

Nekara Skills currently assigns vanilla swords and tridents to Light Weapons,
and axes and the mace to Heavy Weapons by material name. This is insufficient
for original custom weapons, consistent crafting, server-authoritative item
identity, and weapon-specific combat behaviour.

The server needs exactly two combat disciplines with three weapon families each:

| Discipline | Families |
| --- | --- |
| Light Weapons | sword, dagger, spear |
| Heavy Weapons | axe, greatsword, hammer |

Swords, axes and all seven 26.1.2 spear tiers are vanilla items. Daggers,
greatswords and hammers are server-created custom items. The resource pack may
later add the corresponding item models, but gameplay identity must not depend
on a texture, lore text, or client-side state.

## Decision

### Identity and catalog

Every custom weapon receives an immutable PDC identifier, item schema version,
and reserved resource-model key. The catalog is the single source for family,
discipline, material tier, crafting ingredients, base attributes, and combat
profile. Unknown or malformed custom PDC data is not treated as a weapon.

Vanilla swords, axes and spears remain usable through a material-based catalog
fallback. A vanilla mace is intentionally not part of the Nekara weapon catalog.

### Families

| Family | Discipline | Combat role |
| --- | --- | --- |
| Sword | Light Weapons | universal baseline with bleed affinity |
| Dagger | Light Weapons | fast critical weapon with rear-attack bonus |
| Spear | Light Weapons | penetration-oriented weapon; requires empty offhand |
| Axe | Heavy Weapons | slower high-damage critical weapon |
| Greatsword | Heavy Weapons | slower two-handed bleed weapon with bounded cleave |
| Hammer | Heavy Weapons | slower two-handed armor penetration and stun weapon |

Greatswords, hammers, and spears require an empty offhand for their weapon
profile and skill effects. They remain ordinary items if that condition is not
met, rather than deleting a shield or altering player inventory.

### Combat and feedback

Weapon effects are applied inside the existing guarded combat pipeline after
the skill runtime snapshot is loaded. Secondary cleave damage uses the existing
synthetic-combat guard, never grants additional XP, and has a small bounded
radius. Bleed, stun, critical, cleave, rear strike, and penetration feedback
uses local sounds and particles only after the relevant effect succeeds.

### Crafting and quality

The three custom families use registered shaped recipes for all seven vanilla
material tiers. Recipe output is produced by the server-side factory and is
therefore PDC-stamped before a player receives it. Existing Smithing Tier and
workshop processing applies to custom weapons just like vanilla weapons.

`A` means the tier's crafting ingredient, `S` a stick and `B` a second tier
ingredient. The hammer deliberately uses its cross-head pattern rather than
the vanilla pickaxe pattern:

| Weapon | Crafting grid |
| --- | --- |
| Greatsword | `AAA` / ` B ` / ` S ` |
| Hammer | ` A ` / `ASA` / ` S ` |

Equipment material tiers are gated by Smithing level at craft preview and at
the final server-side craft event: wooden and leather at 0, stone at 5, copper
at 10, golden at 15, iron and chainmail at 20, diamond at 50, and netherite at
80. The level gate applies to weapons, tools, armor and custom weapon recipes.

## Consequences

- Perks continue to resolve through `LIGHT_WEAPONS` and `HEAVY_WEAPONS`; they do
  not need duplicate skill trees per weapon family.
- Balance values are centralized in the catalog/configuration and can be tuned
  without migrating player items.
- Future resource-pack work only maps the reserved item-model keys. It does not
  redefine gameplay identity or recipes.
- The implementation requires focused unit tests for identity, classification,
  recipes, offhand rules, rear attacks, bounded cleave, and effect provenance.
