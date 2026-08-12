# ADR 0009: GUI editor and server authority for custom items

## Status

Accepted.

## Context

NekaraRPG needs administrator-created weapons, armor, and general items without a
command language for individual fields. Item identity must survive renaming and
must not depend on the resource pack. Minecraft 26.1 supports namespaced
`item_model` values, while older pack content may still use numeric
`CustomModelData`.

## Decision

- `/nrpg item create` is only an entry point. ID, name, vanilla base material,
  model, legacy model number, and attributes are edited in inventory and anvil
  GUIs.
- `nekararpg:custom_item_id` in PDC is the stable server identity. Display name,
  material, and texture are presentation and may change independently.
- The primary pack contract is `item_model: nekararpg:<model-key>`. Numeric
  `CustomModelData` remains optional for migration and legacy assets.
- Definitions are stored in `plugins/NekaraRPG/custom-items/items.yml`; duplicate
  IDs are rejected and existing definitions are never silently overwritten.
- Attributes use namespaced Bukkit modifiers and the equipment slot inferred from
  the vanilla base item. Attack attributes are main-hand only.
- The `custom-items` module owns its listeners, drafts, prompts, and storage
  lifecycle and closes all editor state when disabled or reloaded.

## Consequences

The resource pack can add or replace a model without changing server identity.
Administrators can create testable items without entering complex commands. A
future catalog/editor can reuse the persisted definitions and factory without
changing already issued item IDs.
