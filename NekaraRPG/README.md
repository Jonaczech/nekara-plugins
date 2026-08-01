# NekaraRPG

NekaraRPG is a modular Purpur/Paper plugin for Nekara RPG and immersion systems.
The first production module is `fishing`, migrated from NekaraFishing.

The plugin is intentionally conservative: it does not replace vanilla loot
tables, does not generate synthetic fishing events, and does not require
ValhallaMMO or any other plugin.

## Modules

Modules are enabled by default and can be controlled in `config.yml`:

```yml
modules:
  fishing:
    enabled: true
```

Current modules:

| Module | Status | Description |
| --- | --- | --- |
| `fishing` | production | Non-invasive fishing timing minigame with vanilla/ValhallaMMO compatibility. |

Future modules can be added without turning the plugin into separate unrelated
JARs. Good candidates are lockpicking, wounds, campfire/rest, world events,
rumors, territory ambience, and reputation.

## Fishing Compatibility Mode

The fishing module uses `DEFERRED_CATCH`, matching the intended LiteFish-like
player flow while preserving the original server-generated catch:

1. Vanilla/Paper emits `PlayerFishEvent.State.BITE` and NekaraRPG creates a pending UUID-owned fishing session.
2. The player's next rod right-click proceeds through normal Minecraft fishing and produces the real `PlayerFishEvent.State.CAUGHT_FISH`.
3. NekaraRPG cancels that event after other normal listeners had a chance to observe it, stores the exact original caught ItemStack and vanilla XP value, removes the temporary item entity, and starts the action-bar minigame.
4. On success, the stored original ItemStack is delivered to the player and the stored vanilla XP is restored; no synthetic fishing event, loot table, or custom item is created.
5. On failure, timeout, disconnect, teleport, or invalid session state, the temporary catch is discarded and no loot is delivered.

The first click after the bite creates the deferred catch and starts the
minigame; it is not counted as a successful timing hit. Each later successful
hit adds `minigame.time-bonus-ticks` to the timer.

During gameplay, chat is intentionally quiet: by default only the bite and final
escape messages are sent. Hits and progress are represented through the action
bar, boss bar, particles, and sounds.

## ValhallaMMO

ValhallaMMO is a soft dependency. When installed, NekaraRPG can:

- scale fishing minigame difficulty from the player's ValhallaMMO FishingSkill level,
- defer ValhallaMMO fishing profession XP so it is awarded alongside the final catch,
- preserve prepared ValhallaMMO extra drops such as double-loot output.

Loot, ValhallaMMO XP, and skill progression are not replaced or recalculated by
NekaraRPG. Disable difficulty scaling with:

```yml
valhalla:
  fishing-difficulty:
    enabled: false
```

## Requirements

- Purpur 26.1.2 or compatible Paper API implementation
- Java 25
- No required plugin dependencies

The plugin uses the Purpur API as `compileOnly`, so the output is not a fat JAR
and does not bundle Paper/Purpur classes.

## Build

From this directory:

```text
gradlew.bat clean test build
```

The build produces:

- `build/libs/NekaraRPG-1.0.0.jar`
- `dist/NekaraRPG-1.0.0.jar`
- repository-level `../dist/NekaraRPG-1.0.0.jar`

## Installation

Copy the JAR from `dist/` to the server's `plugins/` directory, start Purpur,
then configure:

- `plugins/NekaraRPG/config.yml`
- `plugins/NekaraRPG/messages.yml`

Use `/nekararpg reload` after configuration changes. A reload safely ends active
fishing sessions and does not register duplicate listeners or ticker tasks.

## Commands and Permissions

| Command | Permission |
| --- | --- |
| `/nekararpg help` | `nekararpg.command.help` |
| `/nekararpg reload` | `nekararpg.command.reload` |
| `/nekararpg status` | `nekararpg.command.status` |
| `/nekararpg test` | `nekararpg.command.test` |
| `/nekararpg cancel [player]` | `nekararpg.command.cancel` |

Aliases: `/nrpg`, `/nekarafishing`, `/nfishing`.

Fishing requires `nekararpg.use`, which defaults to true for every player.
`nekararpg.bypass` skips the fishing minigame and defaults to false.

Legacy `nekarafishing.*` permissions are still declared and accepted so existing
server permission setups can migrate gradually.

## Sounds and Messages

Sounds support vanilla namespaced IDs and custom resource-pack IDs such as
`nekara:fishing.hit`. Vanilla IDs are checked against the API registry; custom
IDs are syntax-validated and left to the client resource pack. Invalid IDs are
logged and skipped.

Messages use `messages.yml`, legacy `&` colors, and MiniMessage tags when tags
are present. The action bar is built internally and has no PlaceholderAPI
dependency.

## Limitations

- The fishing module is intentionally an action-bar timing gate; it does not replace Minecraft's fishing mechanics.
- The minigame starts on the rod click after a bite and defers the original `CAUGHT_FISH` Item until the final hit.
- A client-side resource-pack sound cannot be verified from the server; only its namespaced syntax can be validated.
- Full acceptance still requires a live Purpur 26.1.2 server because Bukkit event ordering and other plugin interactions cannot be completely simulated by pure unit tests.

## License

MIT-style project license; see `LICENSE`.
