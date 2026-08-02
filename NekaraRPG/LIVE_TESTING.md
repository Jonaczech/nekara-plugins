# NekaraRPG Live Testing

## Recommended Setup

Use a separate Purpur 26.1.2 staging server with Java 25. Keep its world and port
separate from production. Test in two passes:

1. Purpur plus NekaraRPG only, to isolate plugin behavior.
2. The same server with CMI and ValhallaMMO, to verify compatibility.

Build and deploy from the `NekaraRPG` directory:

```powershell
scripts\build-release.cmd
scripts\deploy-test.cmd -ServerPath D:\path\to\test-server
```

Stop the server before replacement and restart it afterwards. Do not use Bukkit
`/reload` to replace a JAR. `/nekararpg reload` is supported only for reloading
NekaraRPG configuration and messages. The deploy script refuses to continue if
another `NekaraRPG*.jar` or legacy `NekaraFishing*.jar` is present beside the
stable `NekaraRPG.jar`; move the reported file outside `plugins` first.

## Fast Iteration Profile

For a short development cycle, temporarily use these values on the staging server:

```yml
campfire:
  update-period-ticks: 10
  healing:
    amount: 2.0
    period-seconds: 1
  hunger:
    restore-amount: 1
    restore-period-seconds: 2
  rested:
    charge-seconds: 5
    duration-seconds: 20
  camping:
    duration-per-feature-seconds: 10
    spawn-protection:
      radius: 10.0
```

Run `/nekararpg reload` after changing these values. Restore production defaults
before acceptance: 20-second charge, five-minute base duration, one minute per
unique camping feature, a 24-block safe-camp radius, one health point every
five seconds, and one food point every ten seconds.

## Sitting Acceptance

1. Run `/nekararpg sit` on full blocks, slabs, stairs, and uneven terrain.
2. Verify the seated pose rests directly on the surface with the migrated `0.20` offset, without intersecting or visibly floating above the block, and `/nekararpg stand` removes the seat.
3. Sit again and use the normal dismount key; verify the invisible seat disappears.
4. Verify teleport, death, disconnect, damage, plugin reload, and shutdown leave no armor stands behind.
5. With CMI installed, verify its top-level `/sit` behavior is unchanged. NekaraRPG deliberately registers only `/nekararpg sit` and `/nrpg sit`.
6. Use `/cmi sit` (or the configured CMI `/sit` alias) near a fire and verify Campfire starts without first using a NekaraRPG command.
7. Disable `modules.sitting.enabled`, reload, and repeat the CMI test. External seating must continue to power Campfire.

## Campfire Acceptance

1. Damage the player and lower the hunger bar, then sit within the spherical five-block default radius of a lit campfire.
2. Verify health rises at the configured interval, hunger does not fall, hunger slowly restores, and the active-rest particles appear.
3. Extinguish or break the campfire and verify active resting ends on the next update.
4. Repeat with a lit soul campfire and verify the same behavior.
5. Sit outside the configured radius and verify no campfire effects start.
6. Wait for the full real-time charge at a bare fire and verify the one-time Rested message, soft amethyst chime, and text-only `Odpočatý | m:ss` action-bar timer, but no Haste. It must never display a raw message key or create a bossbar.
7. Leave the bare fire and create hunger loss; the bar should fall at its normal rate even though Rested remains active.
8. Add a smoker within five blocks, recharge Rested, and verify hunger falls at the configured average multiplier after leaving.
9. Add a crafting table within five blocks, recharge Rested, and verify one extra minute plus Haste I and its top-right potion icon.
10. Add each remaining feature type one at a time and verify each unique type adds one minute while duplicates add nothing.
11. For a quick expiry test, temporarily shorten the durations; verify the timer disappears, normal hunger loss returns, and managed Haste disappears.
12. Give the player a stronger Haste effect before resting and verify NekaraRPG does not replace or remove it.

## Safe Camp Acceptance

1. Place a bed within five blocks of a lit campfire and move around its default 24-block protection radius.
2. Allow night-time natural spawning and verify vanilla hostile mobs do not spawn inside the radius but can still walk in from outside.
3. Leave the area so its chunks unload, return later, and verify the unchanged bed and lit campfire protect the camp again without rebuilding it.
4. Extinguish the fire or move the bed outside the five-block feature radius and verify natural hostile spawning resumes.
5. With MythicMobs installed, verify natural `NekaraHostile` random spawns are blocked while `NekaraFauna` continues to spawn.
6. Trigger a command, summon, quest, or boss spawn inside the camp and verify it is not cancelled.

## Group and Compatibility Acceptance

1. Seat two players near the same campfire and verify the action bar reports two players and a `1.15x` multiplier.
2. Compare healing over the same interval with one and two players.
3. Move one player to a different campfire and verify both groups return to `1.00x`.
4. Run `/nekararpg status` and verify seated, resting, and Rested counts.
5. With ValhallaMMO installed, compare normal and Rested XP for several skills; Rested should grant exactly `1.10x` normal skill-action and shared XP.
6. Complete a Rested fishing minigame and verify the original loot is unchanged and deferred fishing XP receives the 10% bonus exactly once.
7. Grant ValhallaMMO XP through an administrator command and verify it is not multiplied.
8. Disable each module independently and reload. With Sitting disabled, Campfire should still accept a configured external seat but not `/nrpg sit`.
9. Start the fishing minigame while Rested is active and verify its timing UI replaces the Rested timer until the minigame ends.

Future camp-quality calculation can be anchored to the existing per-campfire
group key and add nearby camp structures without changing the sitting contract.

## Echo Vein Acceptance

1. Use `/nekararpg test vein` in several cave shapes and verify the selected block is visible, reachable, shown only to the test player, and limited to stone, deepslate, netherrack, or end stone.
2. Confirm the immediate one-block area has a subtle pulse while the visible target face is markedly denser. Ores, dirt, gravel, and wood must never be selected or trigger an automatic echo.
3. Set trigger chance to `1.0`, mine a host block with both a new and experienced Valhalla Mining profile, and verify the echo starts at either level only after the original mining rewards finish.
4. Verify natural chat remains empty, the low discovery chime plays once, and the action-bar timer appears.
5. Mine several non-target blocks and verify the active target and timer remain. Mine the target and verify one success sound when no chain starts.
6. Enable `plugin.debug`. Compare `markedBlockXp`, `bonusXp`, and the Valhalla profile delta: the bonus must equal 25% of that marked block's final amount, occur once, and use reason `PLUGIN`.
7. Inspect the bonus item. It must be one exact item from the marked block's finalized natural or Valhalla-prepared output, including metadata, never a rerolled Fortune stack. `/nekararpg test vein` must grant none.
8. Force ore reveal to `1.0` and test Y 70, Y 15, Y -32, and Y -64. Verify every result belongs to its vanilla-compatible height band and no high diamond/redstone can appear.
9. Compare normal and Badlands Y 70 for high gold. Test netherrack just outside and inside Y 10-117. End stone must remain unchanged.
10. Verify successful ore transformation plays its brighter amethyst sound once and is clearly distinct from vein discovery. Repeated damage must not replay it.
11. Force chaining to `1.0`, mine a target next to a visible host, and verify the next target is face-adjacent and receives its own timer/rewards. Then restore `0.50`.
12. Let a natural attempt expire and verify it produces no chat or failure sound.
13. Test Rested, Silk Touch, full inventory overflow, disconnect, death, teleport, and fishing priority.
14. Verify both the new `modules.mining.enabled` toggle and fallback from the legacy `modules.echo-vein.enabled` value.
15. Restore defaults (`0.05` chance, no cooldown, `0.50` chain, `0.25` ore reveal) and verify consecutive eligible blocks remain independent.
16. Disable ValhallaMMO or its Mining skill and verify NekaraRPG starts fail-soft while automatic Echo Vein triggers remain unavailable.

## Updater Acceptance

Use a disposable staging release newer than the installed plugin. Its only
asset must be the stable `NekaraRPG.jar` produced by `build-release.cmd`.

1. Run `/nekararpg update check` and verify the command responds immediately while the download continues asynchronously.
2. Verify the console reports the staged version and an operator with `nekararpg.update.notify` receives the Czech restart notice.
3. Confirm the downloaded file is in Paper's configured update folder, not beside the active plugin JAR.
4. Run `/nekararpg update status`, reconnect the operator, and verify both report that the same version is waiting for restart.
5. Stop and start the server normally. Verify the staged file is consumed, exactly one active `NekaraRPG.jar` remains, and `/nekararpg status` reports the new version.
6. Inspect startup logs and rerun the fishing, Sitting, Campfire, Echo Vein, ValhallaMMO, and MythicMobs smoke checks before production rollout.

Never test updater failure modes against production. Use a staging release or a
temporary repository fixture for wrong digests, identities, sizes, and versions.
