# NekaraFishing

NekaraFishing is a small, non-invasive fishing timing minigame for Purpur 26.1.2. It does not add loot, fish, economy, rarity, inventories, or a replacement fishing system.

## Compatibility mode

The plugin uses `DEFERRED_CATCH`, matching the intended LiteFish-like player flow while preserving the original server-generated catch:

1. Vanilla/Paper emits `PlayerFishEvent.State.BITE` and NekaraFishing creates a pending UUID-owned session.
2. The player's next rod right-click proceeds through normal Minecraft fishing and produces the real `PlayerFishEvent.State.CAUGHT_FISH`.
3. NekaraFishing cancels that event after other normal listeners had a chance to observe it, stores the exact original caught ItemStack and vanilla XP value, removes the temporary item entity, and starts the action-bar minigame.
4. On success, the stored original ItemStack is delivered to the player and the stored vanilla XP is restored; no synthetic fishing event, loot table, or custom item is created.
5. On failure, timeout, disconnect, teleport, or invalid session state, the temporary catch is discarded and no loot is delivered.

The first click after the bite creates the deferred catch and starts the minigame; it is not counted as a successful timing hit. The configured number of later hits is required before delivery. Each successful hit adds `minigame.time-bonus-ticks` to the current timer (30 ticks by default), so a good player receives extra time to finish the sequence.

After a successful hit, the next target remains random but must be reachable along the current indicator trajectory within `minigame.target-relocation-max-distance` (6 movement steps by default). This avoids an unfair full-bar wait while preserving the moving-indicator challenge.

During the active minigame, a configurable particle ring is rendered around the real bobber for the fishing player only. It uses the existing main-thread ticker, stops with the session, and can be configured under `minigame.hook-particles` or disabled entirely.

The bobber also receives a short configurable outcome effect: green `HAPPY_VILLAGER` particles on success and red `DAMAGE_INDICATOR` particles when the fish escapes. The success effect is shown before the original catch and ValhallaMMO reward are delivered; it never changes the catch itself. Configure these under `effects.success` and `effects.failure`.

The action bar intentionally contains no redundant `FISH` label. It uses a compact bar with a gold moving indicator, green target cells, and a timer that turns red near timeout. The renderer is built into the plugin and does not require a resource pack or PlaceholderAPI.

Its current visual language is `━━━━⚓━━━━ 2.3s`: the `━` glyph is used for both the track and target zone, with the target rendered bold and green, while the gold `⚓` is the moving indicator. The timer remains at the right and turns red near timeout.

The active UI uses one action bar for timing and a separate vanilla BossBar above it for hit progress. Minecraft only supports one action bar at a time, so this gives the requested two-level layout without one action bar overwriting the other. The timing bar uses a thick line track, while the BossBar fills after each successful pull.

Each session randomly selects the required number of successful pulls between `minigame.required-hits-min` and `minigame.required-hits-max` (3 to 5 by default). The selected value is used consistently for completion, progress, and message placeholders.

After each successful pull, the real bobber moves toward the player by `minigame.hook-pull-distance` blocks (1.0 by default). A Paper block ray trace limits the movement before solid obstacles; setting the value to `0` disables the pull effect.

When ValhallaMMO is installed, fishing difficulty scaling is enabled by default. NekaraFishing reads only the player's ValhallaMMO FishingSkill level through reflection and applies configurable tiers: levels 1–30 use 3–5 required pulls and 1 miss, levels 31–60 use 3–4 pulls and 2 misses, and level 61+ uses 2–3 pulls and 3 misses. At the actual ValhallaMMO max level, the configured max-level override uses exactly 2 pulls and 3 misses. Loot, ValhallaMMO XP, and skill progression are not replaced or recalculated. Disable this behavior with `valhalla.fishing-difficulty.enabled: false`.

During gameplay, chat is intentionally kept quiet: only the bite message and the final escape message are sent. Hits, misses, minigame completion, successful catch, timeout, and session cancellation are represented by the action bar and/or sounds.

The main compatibility boundary is the `CAUGHT_FISH` event. Plugins that completely replace vanilla fishing or cancel that event before NekaraFishing can capture it can prevent the final catch. ValhallaMMO and other listeners still receive the original event according to their event priority; NekaraFishing does not generate a synthetic `PlayerFishEvent` or replacement loot table.

While a session is pending or active, additional `BITE` events are cancelled so a second minigame cannot start. An unexpected second `CAUGHT_FISH` is also cancelled; if the original deferred catch is still present, it is preserved and the current minigame continues.

After the original catch is captured, the hook's public Paper fishing state is reset and its next-bite wait is temporarily extended while the minigame is active. This prevents a new vanilla fish animation/event from competing with the minigame; the hook is removed normally when the session ends.

When ValhallaMMO 1.12.x is installed, NekaraFishing also registers an optional reflection-based bridge for ValhallaMMO's public `PlayerSkillExperienceGainEvent`. Fishing profession XP belonging to the deferred catch is held back and replayed through ValhallaMMO's public skill API at the same time as the original ItemStack is delivered. If the bridge is unavailable, ValhallaMMO remains untouched and keeps its normal event timing. Vanilla XP is always handled separately from ValhallaMMO profession XP.

The same bridge captures ValhallaMMO's prepared extra fishing drops (for example double-loot skill output) before ValhallaMMO's cancelled-event monitor listener can discard them. Those exact original ItemStacks are delivered only after minigame success; no replacement loot is generated by NekaraFishing.

## Requirements

- Purpur 26.1.2 or compatible Paper API implementation
- Java 25 (Paper lists Java 25 for 26.1+)
- No required plugin dependencies

The plugin uses only the Purpur API at compile time. The API is `compileOnly`, so the output is not a fat JAR and does not bundle Paper/Purpur classes.

## Build

From this directory:

```text
gradlew.bat clean test build
```

The build produces `build/libs/NekaraFishing-1.0.0.jar`, copies it to this project's `dist/NekaraFishing-1.0.0.jar`, and mirrors it to the repository-level `../../dist/NekaraFishing-1.0.0.jar`.

## Installation

Copy the JAR from `dist/` to the server's `plugins/` directory, start Purpur, then configure:

- `plugins/NekaraFishing/config.yml`
- `plugins/NekaraFishing/messages.yml`

Use `/nekarafishing reload` after configuration changes. A reload safely ends active sessions and does not register duplicate listeners or ticker tasks.

## Commands and permissions

| Command | Permission |
| --- | --- |
| `/nekarafishing help` | `nekarafishing.command.help` |
| `/nekarafishing reload` | `nekarafishing.command.reload` |
| `/nekarafishing status` | `nekarafishing.command.status` |
| `/nekarafishing test` | `nekarafishing.command.test` |
| `/nekarafishing cancel [player]` | `nekarafishing.command.cancel` |

Fishing requires `nekarafishing.use`, which defaults to true for every player, including operators. `nekarafishing.bypass` skips the minigame but defaults to false, so operators also play the minigame unless an administrator explicitly grants the bypass permission. The default world mode is `ALL`; administrative permissions default to operators.

## Sounds and messages

Sounds support vanilla namespaced IDs and custom resource-pack IDs such as `nekara:fishing.hit`. Vanilla IDs are checked against the API registry; custom IDs are syntax-validated and left to the client resource pack. Invalid IDs are logged and skipped.

Messages use `messages.yml`, legacy `&` colors, and MiniMessage tags when tags are present. The action bar is built internally and has no PlaceholderAPI dependency.

## Limitations

- The minigame is intentionally an action-bar timing gate; it does not replace Minecraft's fishing mechanics.
- The minigame starts on the rod click after a bite and defers the original `CAUGHT_FISH` Item until the final hit.
- A client-side resource-pack sound cannot be verified from the server; only its namespaced syntax can be validated.
- Full acceptance still requires a live Purpur 26.1.2 server because Bukkit event ordering and other plugin interactions cannot be completely simulated by pure unit tests.

## License

MIT-style project license; see `LICENSE`.
