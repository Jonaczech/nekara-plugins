# NekaraRPG

NekaraRPG is a modular Purpur/Paper plugin for Nekara RPG and immersion systems.
It currently ships fishing, free-position sitting, and campfire rest in one JAR.

The plugin is intentionally conservative: it does not replace vanilla loot
tables, does not generate synthetic fishing events, and does not require
ValhallaMMO or any other plugin.

## Modules

Modules are enabled by default and can be controlled in `config.yml`:

```yml
modules:
  fishing:
    enabled: true
  sitting:
    enabled: true
  campfire:
    enabled: true
```

Current modules:

| Module | Status | Description |
| --- | --- | --- |
| `fishing` | production | Non-invasive fishing timing minigame with vanilla/ValhallaMMO compatibility. |
| `sitting` | production | Command-driven sitting plus configurable detection of external seats. |
| `campfire` | production | Healing, hunger protection, visual Rested bonus, group scaling, and action-bar roleplay near lit campfires. |

Campfire accepts both NekaraRPG seats and configured external vehicle-based
seats. The internal sitting module may therefore be disabled when another
plugin provides the seat. Future modules can be added without turning the
plugin into separate unrelated JARs. Good candidates are lockpicking, wounds,
world events, rumors, territory ambience, and reputation.

## Sitting

Use `/nekararpg sit` to sit at the current grounded position and
`/nekararpg stand` to stand up. The seat is an invisible, non-persistent entity
that is removed on dismount, teleport, death, disconnect, configured damage,
module disable, reload cleanup, and shutdown.

NekaraRPG deliberately does not register the top-level `/sit` command. This
keeps CMI and other existing sitting plugins in control of their own command.
Campfire detects external `ARMOR_STAND` seats by default, so `/cmi sit` can
participate without a compile-time CMI dependency. Additional seat vehicle
types can be added under `sitting.external-seat-entity-types`.

## Campfire Rest

A player actively rests while seated within the configured radius of a lit
campfire or soul campfire. The default radius is `5.0` blocks and uses a true
three-dimensional distance from the player to the fire. Active rest:

- slowly restores health,
- prevents the hunger bar from falling,
- restores a small amount of hunger at a configurable interval,
- scales healing and hunger restoration when players share the same fire,
- shows configurable progress and roleplay messages in the action bar,
- emits low-count particles to confirm that the rest mechanic is active.

After 20 real-time seconds by default, the player receives a five-minute base
Rested bonus. After leaving the fire, Rested reduces average hunger loss to the
configured multiplier, `0.5` by default. The timer uses wall-clock time, so
server lag does not make a twenty-second rest take longer.
Once charged, Rested stays refreshed while the player remains at the fire, then
counts down after the player leaves active rest.

### Camping Equipment

Every unique enabled camping block type within five blocks of the lit campfire
adds one minute to Rested. Duplicate blocks of one type do not stack. The
default set is crafting table, bed, smoker, barrel, water cauldron, cartography
table, and grindstone, giving a possible duration from five to twelve minutes.

A crafting table also grants Haste I for that Rested bonus. Haste supplies
Minecraft's standard potion icon and countdown in the top-right status area.
Existing Haste from another source is not replaced by NekaraRPG.

A bed near the lit campfire creates a configurable 24-block safe-camp radius.
Natural hostile spawns are cancelled while the camp is loaded, so protection
becomes useful again when a player returns. Existing mobs can still walk into
the camp. Command, summon, quest, boss, and other scripted spawns remain
untouched by default. With MythicMobs installed, only natural random spawns in
the configured `NekaraHostile` faction are cancelled; `NekaraFauna` is left
alone. MythicMobs remains a soft dependency and NekaraRPG works without it.

The charging roleplay remains in the action bar. Once charged, Rested shows the
compact action-bar text `Odpočatý | m:ss`, independent of whether Haste is active. The
timer yields to the fishing minigame and to campfire charging messages. Set
`campfire.visuals.rested.indicator` to `NONE` to disable it. Extra particles
remain opt-in. A soft configurable amethyst chime confirms the moment the
20-second charge completes.

The campfire key and public Rested state methods are ready for a later optional
ValhallaMMO XP bridge. XP behavior is not changed in version 1.1.0.

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
- No required plugin dependencies; MythicMobs and ValhallaMMO integrations are optional

The plugin uses the Purpur API as `compileOnly`, so the output is not a fat JAR
and does not bundle Paper/Purpur classes.

## Build

For normal checks:

```text
gradlew.bat clean test build
```

For a verified release:

```text
scripts\build-release.cmd
```

The release produces:

- `build/libs/NekaraRPG.jar`
- `dist/NekaraRPG.jar`
- repository-level `../../dist/NekaraRPG.jar`

The semantic version remains embedded in `plugin.yml`, documented in the
changelog, and represented by the Git tag; it is deliberately omitted from the
deployable filename.

See `DEVELOPMENT.md` for the release contract and `LIVE_TESTING.md` for the
staging-server workflow.

## Installation

Copy the JAR from `dist/` to the server's `plugins/` directory, start Purpur,
then configure:

- `plugins/NekaraRPG/config.yml`
- `plugins/NekaraRPG/messages.yml`

An existing `plugins/NekaraRPG` folder does not need to be deleted during this
upgrade. Missing camping keys use the new runtime defaults. Merge the bundled
`campfire.camping` section into an existing `config.yml` only when those values
need to be customized or documented on the server.

Use `/nekararpg reload` after configuration changes. A reload safely ends active
fishing sessions and does not register duplicate listeners or ticker tasks.

## Commands and Permissions

| Command | Permission |
| --- | --- |
| `/nekararpg help` | `nekararpg.command.help` |
| `/nekararpg reload` | `nekararpg.command.reload` |
| `/nekararpg status` | `nekararpg.command.status` |
| `/nekararpg sit` | `nekararpg.sitting.use` |
| `/nekararpg stand` | none; a player must always be able to stand |
| `/nekararpg test` | `nekararpg.command.test` |
| `/nekararpg cancel [player]` | `nekararpg.command.cancel` |

Aliases: `/nrpg`, `/nekarafishing`, `/nfishing`.

Fishing requires `nekararpg.use`, which defaults to true for every player.
`nekararpg.bypass` skips the fishing minigame and defaults to false.
Campfire effects require `nekararpg.campfire.use`, which defaults to true.

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
- Sitting uses a server-side passenger seat; it does not add a client keybind. Its default vertical offset is configurable and existing pre-release offsets are migrated to `0.20` at runtime.
- External seat detection is vehicle-type based. `ARMOR_STAND` is the safe default; plugins using another seat entity must add that type to configuration.
- Campfire and Rested action-bar feedback can temporarily replace other non-priority action-bar text. NekaraRPG's fishing minigame always takes priority over the Rested timer.
- Rested uses a text-only action-bar timer; crafting-table camps additionally use the vanilla Haste icon. A uniquely branded status icon would require a client resource-pack or mod solution.
- Rested currently affects hunger; crafting-table camps also grant configurable Haste. ValhallaMMO XP scaling is reserved for a later compatibility bridge.
- The minigame starts on the rod click after a bite and defers the original `CAUGHT_FISH` Item until the final hit.
- A client-side resource-pack sound cannot be verified from the server; only its namespaced syntax can be validated.
- Full acceptance still requires a live Purpur 26.1.2 server because Bukkit event ordering and other plugin interactions cannot be completely simulated by pure unit tests.

## License

MIT-style project license; see `LICENSE`.
