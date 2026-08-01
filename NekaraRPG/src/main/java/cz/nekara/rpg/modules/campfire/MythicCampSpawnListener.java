package cz.nekara.rpg.modules.campfire;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.events.MythicMobPreSpawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.Listener;

final class MythicCampSpawnListener implements Listener {
    private final CampfireModule campfire;

    MythicCampSpawnListener(CampfireModule campfire) {
        this.campfire = campfire;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMythicMobPreSpawn(MythicMobPreSpawnEvent event) {
        MythicMob mob = event.getMobType();
        String faction = mob.hasFaction() ? mob.getFaction() : null;
        CreatureSpawnEvent.SpawnReason spawnReason = event.getSpawnReason() == null
                ? CreatureSpawnEvent.SpawnReason.CUSTOM
                : event.getSpawnReason().getBukkitReason();
        if (campfire.shouldBlockMythicSpawn(event.getLocation(), spawnReason, faction)) {
            event.setCancelled(true);
        }
    }
}
