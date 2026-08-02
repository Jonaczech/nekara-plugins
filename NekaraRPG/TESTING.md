# NekaraRPG Testing Guide

## Automated Checks

Run the verified release workflow:

```text
scripts\build-release.cmd
```

The unit tests cover indicator movement and edge reflection, target boundaries,
hits, misses, timeout, target bounds, state transitions, double completion
prevention, configuration fallback validation, campfire group scaling, and
fractional Rested hunger reduction. Rested ValhallaMMO XP eligibility and
scaling have focused pure-logic coverage as well. Echo Vein tests cover level,
cooldown, chance, XP bonus, and quantity-weighted drop selection. Updater tests cover strict
semantic versions, GitHub release parsing, trusted asset selection, SHA-256,
JAR identity, and embedded release version validation.

## Purpur 26.1.2 Manual Acceptance

Use a clean test server with Java 25 and only the target server plus NekaraRPG
first. Give the test player the relevant permissions and place the player in an
enabled world.

### 1. Startup and Module Config

1. Install the single release artifact `NekaraRPG.jar`.
2. Start the server and verify the plugin creates `plugins/NekaraRPG/config.yml`.
3. Verify `modules.fishing.enabled`, `modules.sitting.enabled`, `modules.campfire.enabled`, and `modules.echo-vein.enabled` are true by default.
4. Run `/nekararpg status` and verify all four modules are listed.
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

### 3a. ValhallaMMO Rested XP Bonus

1. Confirm ValhallaMMO is installed and `campfire.rested.valhalla-experience.enabled` is `true`.
2. Gain the same amount of skill-action XP without Rested and with Rested; the Rested amount should be exactly `1.10x` by default.
3. Repeat with several skills, including Fishing and one combat or gathering skill. The bonus must not filter by skill type.
4. Gain shared party XP and verify it receives the same bonus.
5. Grant XP by an administrator command and verify the amount is not multiplied.
6. Complete a Rested fishing minigame and verify its deferred XP receives one 10% bonus, not two.
7. Disable the camping module or the Rested XP setting and verify normal ValhallaMMO XP resumes.

### 3b. Echo Vein

1. Confirm ValhallaMMO Mining is enabled and give the player Mining level 50.
2. Run `/nekararpg test vein` in a cave with a visible solid wall. Verify one nearby block pulses for six seconds and the command states that no reward will be granted.
3. Hit the pulsing block with a pickaxe and verify the reward-free test succeeds. Repeat and hit another block, then let a test time out; both should fail cleanly.
4. Temporarily set `echo-vein.trigger-chance` to `1.0` and `cooldown-seconds` to `0`, reload, then mine a natural XP-granting Mining block.
5. Verify the original block and all normal Valhalla drops arrive before the echo starts.
6. Complete the echo and verify the player receives exactly 25% of the final triggering Mining XP plus at most one item selected from that action's finalized drops.
7. Repeat while Rested and verify the source XP contains the Rested bonus, but the 25% echo reward is not multiplied by Rested again.
8. Restore the eight-minute cooldown, trigger an echo, reconnect, and verify another echo cannot trigger until the stored cooldown expires.
9. Verify placed blocks or blocks that grant no Valhalla Mining XP do not trigger Echo Vein.
10. Begin fishing during an echo and verify the echo yields without affecting fishing.

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

### 11. Sitting

1. Run `/nekararpg sit` and verify the player assumes a seated pose at the current grounded position.
2. Verify the player model touches the supporting surface without intersecting or visibly floating above it.
3. Run `/nekararpg stand`, then repeat using the normal dismount key.
4. Verify teleport, death, disconnect, damage, reload, and shutdown remove the invisible seat.
5. Verify sitting fails cleanly while flying, swimming, gliding, sleeping, or already riding another entity.
6. Install CMI and verify its top-level `/sit` command is unchanged; NekaraRPG uses only its own subcommand namespace.
7. Use `/cmi sit` and verify `/nekararpg status` includes the externally seated player.

### 12. Campfire Rest

1. Lower health and hunger, sit within the configured radius of a lit campfire, and verify slow healing.
2. Verify hunger does not decrease while resting, rises at the configured interval, and active-rest particles appear.
3. Extinguish or break the fire and verify active rest ends within one update period.
4. Repeat with a lit soul campfire and outside the configured radius.
5. Wait 20 real-time seconds at a bare fire and verify one soft amethyst chime plays, the text-only action bar reads `Odpočatý | 5:00`, no Rested bossbar appears, and no Haste is granted. An older server `messages.yml` must not cause the raw `campfire-rested-timer` key to appear.
6. Leave the bare fire and verify hunger falls at its normal rate while Rested remains active.
7. Add a smoker, recharge Rested, leave the fire, and verify hunger falls at half the normal average rate until Rested expires.
8. Add a crafting table and verify the next Rested bonus gains one minute and Haste I with its top-right potion icon.
9. Add the other configured camping features and verify each unique type adds one minute, while duplicate blocks do not stack.
10. Add a bed and verify natural hostile spawns are blocked within 24 blocks of the lit fire, including after leaving and returning to the camp.
11. Verify existing mobs can enter, Mythic `NekaraFauna` still spawns, and command, summon, quest, and boss spawns are unaffected.
12. Shorten the duration for testing and verify the Rested timer disappears on expiry, reload, and plugin shutdown; managed Haste must disappear with it.
13. Apply a stronger external Haste effect before resting and verify it is preserved.

### 13. Group Rest

1. Seat two players near one campfire and verify the action bar shows two players and a `1.15x` multiplier.
2. Verify both players remain independent and receive stronger healing than a solo player.
3. Move one player to another fire and verify each campfire becomes a separate `1.00x` group.
4. Verify `/nekararpg status` reports the expected seated, resting, and Rested counts.

### 14. Module Dependencies and Reload

1. Disable `modules.campfire.enabled`, reload, and verify sitting remains available without campfire effects.
2. Re-enable campfire, disable sitting, reload, and verify Campfire still works through a configured CMI seat.
3. Re-enable both modules and verify no duplicate listeners, action-bar streams, or scheduler effects appear.

### 15. GitHub Updater

1. Install 1.2.0 manually; versions before it cannot update themselves.
2. Run `/nekararpg update status` and verify it reports an idle or completed state without blocking the server tick.
3. Run `/nekararpg update check` while the published release is the same or older and verify it reports the installed version as current.
4. Publish a newer staging release with the exact `NekaraRPG.jar` asset and verify the plugin first creates a hash-matching rollback JAR under `plugins/NekaraRPG/backups`, then creates `plugins/update/NekaraRPG.jar` only after release digest and identity validation.
5. Verify an online operator receives the staged-update message and receives it again after reconnecting before restart.
6. Run the check twice and verify only one network operation runs at a time and the already verified staged JAR is reused.
7. Restart the server fully and verify Paper replaces the active JAR, removes the staged copy, loads the new embedded version, and preserves `plugins/NekaraRPG/config.yml`.
8. Repeat with an invalid digest, wrong asset name, mismatched manifest version, oversized JAR, unavailable GitHub API, and timeout; every case must fail without touching the active JAR.
9. Disable `updater.automatic-checks`, reload, and verify manual checks still work. Disable `updater.enabled` and verify both automatic and manual checks stop.

For the staged deployment workflow, accelerated timer profile, CMI pass, and
ValhallaMMO compatibility pass, see `LIVE_TESTING.md`.
