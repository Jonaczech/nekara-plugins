# Skill resource layout

Every trainable skill owns a folder with its runtime configuration and player-facing messages.
Gathering skills may also own a `loot-tables.yml`. Shared progression, storage and queue controls
remain in `skills/config.yml`.

At startup, legacy values from the former monolithic `skills/config.yml` are migrated into these
folders without overwriting an existing per-skill file.
