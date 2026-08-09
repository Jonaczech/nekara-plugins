# ADR 0008: Server-authoritative flying dragon mount prototype

- Status: Accepted for prototype
- Date: 2026-08-09

## Context

NekaraRPG already owns the persistent horse lifecycle through the `mounts` module. The next progression milestone introduces one personal flying dragon per player at Power level 100. A player may own both companions, but only one horse or dragon may exist actively in the world at a time.

The first milestone must validate gameplay without requiring a client mod. It therefore needs a rideable vanilla server entity while retaining server authority over access, movement, summoning, switching and cleanup.

## Decision

- Keep the feature in the NekaraRPG JAR as a separate configurable `dragons` module.
- Use an invisible adult harnessed Happy Ghast as the rideable carrier and a synchronized vanilla Ender Dragon as its server-spawned visual.
- Hide the carrier harness from clients and drive the carrier from the rider's server-visible input: WASD for horizontal movement, jump to ascend and sneak to descend.
- Keep the Ender Dragon in a passive hover phase and block its damage, explosions and block changes. It is a visual model only.
- Treat Power milestone `dragon_bond` at level 100 as implicit ownership of exactly one prototype dragon.
- Introduce one shared `ActiveMountCoordinator` for the horse and dragon modules. Calling one mount first deactivates the other.
- Preserve the horse's existing persistence guarantee: its current state must be stored successfully before the horse entity is removed. If storage fails, switching fails and the horse remains in the world.
- Keep the prototype dragon ephemeral. It is removed on dismiss, logout, chunk unload, module disable or server restart and can be summoned again after the milestone check.
- Reuse the owner-bound mount whistle. Before Dragon Bond it calls the horse directly; after Dragon Bond it opens a horse-or-dragon selector.
- Recall an active nearby dragon by server-controlled flight to a safe point near the caller. At or beyond the configurable Chebyshev chunk distance (default three chunks), safely teleport it instead.
- Expose `Můj drak` in the central `/nrpg` menu only when the module is enabled and Dragon Bond is unlocked.
- Give the dragon no inventory, equipment management or combat ability. Only its owner can ride it.

## Consequences

The prototype can validate flight speed, control, spawn clearance, altitude, cooldowns, mount switching and GUI flow with no custom client dependency. Both the rideable carrier and the Ender Dragon visual are created and controlled exclusively by NekaraRPG on the server.

Persistent dragon identity, name, cosmetics and progression are deferred. The invisible Happy Ghast remains the physical collision and passenger carrier, so safe spawning is deliberately conservative and requires live-server tuning. The visual Ender Dragon is synchronized by teleportation and is not a native steerable vehicle.
