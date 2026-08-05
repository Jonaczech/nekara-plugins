# ADR 0003: ASCII migration of public skill identifiers

## Decision

Public skill identifiers use the Czech ASCII forms `runotepectvi`, `tezba`,
`lesnictvi`, `kopani`, `statkarstvi`, `rybareni`, and `lehke_zbrane`.
Legacy identifiers are not accepted as command aliases.

## Migration

On first startup schema 5 rewrites `skill_experience`, `skill_new_game_plus`,
and the skill prefix in `perk_ranks` in one SQLite transaction. The module
configuration store moves the corresponding per-skill folders before loading
them and migrates old root YAML keys. A target collision fails closed rather
than overwriting configuration or player progress.
