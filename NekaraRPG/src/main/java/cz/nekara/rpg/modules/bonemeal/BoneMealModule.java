package cz.nekara.rpg.modules.bonemeal;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.modules.NekaraModule;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Extends successful bone-meal growth with dye flowers and desert vegetation. */
public final class BoneMealModule implements NekaraModule, Listener {
    public static final String ID = "bone-meal";
    private static final List<String> DESERT_DRY_GRASS = List.of("SHORT_DRY_GRASS", "DRY_GRASS");
    private static final List<String> DESERT_TALL_DRY_GRASS = List.of("TALL_DRY_GRASS");
    private static final List<String> DESERT_CACTUS_FLOWER = List.of("CACTUS_FLOWER");

    private final NekaraRPGPlugin plugin;
    private boolean enabled;

    public BoneMealModule(NekaraRPGPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void enable() {
        if (enabled) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        HandlerList.unregisterAll(this);
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void enrichGrassGrowth(BlockFertilizeEvent event) {
        if (!BoneMealPolicy.isGrass(event.getBlock().getType()) || event.getPlayer() == null || event.getBlocks().isEmpty()) return;
        Block origin = event.getBlock();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (int attempt = 0; attempt < 2; attempt++) {
                placeGrassFlower(origin);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void fertilizeDesertSand(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
            || event.getClickedBlock() == null || event.getMaterial() != Material.BONE_MEAL
            || !BoneMealPolicy.isDesertSand(event.getClickedBlock().getType())) return;
        Block origin = event.getClickedBlock();
        int placed = 0;
        for (int attempt = 0; attempt < 8 && placed < 3; attempt++) {
            if (placeDesertPlant(origin)) placed++;
        }
        if (placed == 0) return;
        event.setCancelled(true);
        consumeBoneMeal(event.getItem(), event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE);
        origin.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, origin.getLocation().add(0.5, 1.0, 0.5), 8, 1.4, 0.2, 1.4, 0.0);
        origin.getWorld().playSound(origin.getLocation(), Sound.ITEM_BONE_MEAL_USE, 0.8F, 0.9F);
    }

    private void placeGrassFlower(Block origin) {
        Block target = randomAbove(origin, 3);
        if (target == null) return;
        placePlant(target, random(BoneMealPolicy.dyeFlowers()));
    }

    private boolean placeDesertPlant(Block origin) {
        Block target = randomAbove(origin, 3);
        if (target == null) return false;
        return switch (BoneMealPolicy.desertPlantForRoll(ThreadLocalRandom.current().nextInt(100))) {
            case DEAD_BUSH -> placePlant(target, Material.DEAD_BUSH);
            case SHORT_DRY_GRASS -> placeNamedPlant(target, DESERT_DRY_GRASS);
            case TALL_DRY_GRASS -> placeNamedPlant(target, DESERT_TALL_DRY_GRASS);
            case CACTUS_FLOWER -> placeNamedPlant(target, DESERT_CACTUS_FLOWER);
        };
    }

    private Block randomAbove(Block origin, int radius) {
        int x = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
        int z = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
        Block below = origin.getRelative(x, 0, z);
        Block target = below.getRelative(0, 1, 0);
        return below.getType() == origin.getType() && target.getType().isAir() ? target : null;
    }

    private boolean placeNamedPlant(Block target, List<String> materialNames) {
        for (String materialName : materialNames) {
            Material material = Material.matchMaterial(materialName);
            if (material != null && placePlant(target, material)) return true;
        }
        return false;
    }


    private static boolean placePlant(Block target, Material material) {
        if (!target.getType().isAir()) return false;
        if (material == Material.SUNFLOWER || material == Material.LILAC || material == Material.ROSE_BUSH
            || material == Material.PEONY || material.name().equals("TALL_DRY_GRASS") || material == Material.PITCHER_PLANT) {
            Block upper = target.getRelative(0, 1, 0);
            if (!upper.getType().isAir()) return false;
            target.setType(material, false);
            upper.setType(material, false);
            if (target.getBlockData() instanceof Bisected lower && upper.getBlockData() instanceof Bisected upperData) {
                lower.setHalf(Bisected.Half.BOTTOM);
                upperData.setHalf(Bisected.Half.TOP);
                target.setBlockData(lower, false);
                upper.setBlockData(upperData, true);
                return true;
            }
            target.setType(Material.AIR, false);
            upper.setType(Material.AIR, false);
            return false;
        }
        return setType(target, material);
    }

    private static boolean setType(Block block, Material material) {
        block.setType(material, true);
        return block.getType() == material;
    }

    private static Material firstAvailable(List<String> names) {
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material != null) return material;
        }
        return null;
    }

    private static Material random(List<Material> materials) {
        return materials.get(ThreadLocalRandom.current().nextInt(materials.size()));
    }

    private static void consumeBoneMeal(ItemStack item, boolean consume) {
        if (consume && item != null && item.getType() == Material.BONE_MEAL) item.subtract(1);
    }
}
