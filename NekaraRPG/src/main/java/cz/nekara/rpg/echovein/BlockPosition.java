package cz.nekara.rpg.echovein;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.UUID;

public record BlockPosition(UUID worldId, int x, int y, int z) {
    public static BlockPosition of(Block block) {
        return new BlockPosition(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockPosition of(Location location) {
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Location must have a world");
        }
        return new BlockPosition(
                location.getWorld().getUID(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }
}
