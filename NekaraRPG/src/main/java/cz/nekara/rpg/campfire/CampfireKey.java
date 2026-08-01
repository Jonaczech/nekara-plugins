package cz.nekara.rpg.campfire;

import org.bukkit.block.Block;

import java.util.UUID;

public record CampfireKey(UUID worldId, int x, int y, int z) {
    public static CampfireKey from(Block block) {
        return new CampfireKey(
                block.getWorld().getUID(),
                block.getX(),
                block.getY(),
                block.getZ()
        );
    }
}
