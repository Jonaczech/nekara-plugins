# NekaraRPG Testing Guide

## Automated Checks

Run:

```text
gradlew.bat clean test build
```

The unit tests cover indicator movement and edge reflection, target boundaries,
hits, misses, timeout, target bounds, state transitions, double completion
prevention, and configuration fallback validation.

## Purpur 26.1.2 Manual Acceptance

Use a clean test server with Java 25 and only the target server plus NekaraRPG
first. Give the test player the relevant permissions and place the player in an
enabled world.

### 1. Startup and Module Config

1. Install `NekaraRPG-1.0.1.jar`.
2. Start the server and verify the plugin creates `plugins/NekaraRPG/config.yml`.
3. Verify `modules.fishing.enabled: true` is present by default.
4. Run `/nekararpg status` and verify the `fishing` module is listed.
5. Set `modules.fishing.enabled: false`, run `/nekararpg reload`, and verify fishing minigames no longer start.
6. Re-enable the module and reload again.

### 2. Normal Successful Fishing

1. Cast a fishing rod.
2. Wait for the real bite.
3. Right-click once to create the real catch event and start the action-bar minigame; verify no item is delivered yet.
4. Complete the configured number of hits; verify each successful hit gives back the configured time bonus.
5. Verify exactly one original vanilla catch is delivered after the final hit.
6. Verify vanilla XP remains present and the catch-success sound plays separately from the minigame-success sound.
7. Verify the action bar has no `FISH` label and the BossBar above it fills after each successful pull.
8. Repeat enough sessions to observe different required hit counts between 3 and 5.
9. After each successful pull, verify the real bobber moves toward the player and stops before a solid wall; verify `minigame.hook-pull-distance: 0` disables that movement.

### 3. ValhallaMMO Fishing Difficulty Scaling

1. Confirm ValhallaMMO is installed and `valhalla.fishing-difficulty.enabled` is `true`.
2. Enable debug logging and start a fishing minigame; verify the console reports the player's FishingSkill level and effective required-hit/miss values.
3. Compare players in levels 1-30, 31-60, and 61+. They should receive the configured tier values: 3-5/1 miss, 3-4/2 misses, and 2-3/3 misses respectively.
4. Verify the actual ValhallaMMO max-level player receives the exact override of 2 pulls and 3 misses.
5. Verify the original vanilla/ValhallaMMO loot and fishing XP are unchanged.

### 4. Failed Minigame

1. Cast and wait for a bite.
2. Miss until the configured miss limit is exceeded, or wait for timeout.
3. Verify the escape/timeout feedback appears.
4. Verify no item is created, no replacement loot appears, and the active session count returns to zero.
5. Verify the failure particle effect appears briefly around the bobber and the bobber/session are then cleaned up.

### 5. Disconnect

1. Start the minigame.
2. Disconnect the player.
3. Verify the session is removed and the console contains no exception.

### 6. Teleport

1. Start the minigame.
2. Teleport the player to another location or world.
3. Verify the session is canceled, the hook does not remain active, and the session count returns to zero.

### 7. Reload

1. Start a minigame.
2. Run `/nekararpg reload`.
3. Verify the active session is cleaned up and configuration/messages are reloaded.
4. Start another minigame and verify there is one action-bar update stream, not duplicated updates or sounds.

### 8. Two Players

1. Have two players cast at approximately the same time.
2. Verify their action bars, counters, targets, clicks, and outcomes remain independent.

### 9. Custom Sound

1. Configure a valid resource-pack ID such as `nekara:fishing.hit`.
2. Load a resource pack containing that sound and verify playback.
3. Configure an invalid ID, reload, and verify the plugin logs a warning and continues running.

### 10. Another Plugin Changes Loot

1. Install a test plugin that changes the existing `CAUGHT_FISH` event's `ItemStack` or XP.
2. Complete NekaraRPG fishing.
3. Verify NekaraRPG does not replace the `Item`, alter its metadata, add another item, or change XP.
4. Verify a canceled `CAUGHT_FISH` event does not produce a catch-success message or sound.

## Additional Edge Cases

Also verify off-hand duplicate interactions, changing the held item, dropping
the rod, opening an inventory, death, spectator mode, creative mode, a broken
rod, a missing hook, a caught entity, an already-canceled fishing event, and
server shutdown.
