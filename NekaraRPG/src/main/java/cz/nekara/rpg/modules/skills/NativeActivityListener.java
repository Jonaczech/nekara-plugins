package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.NativeActivityConfig;
import cz.nekara.rpg.fishing.FishingCatchDeliveredEvent;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.experience.ExperienceAwardRequest;
import cz.nekara.rpg.skills.experience.ExperienceContext;
import cz.nekara.rpg.skills.experience.ExperienceFingerprint;
import cz.nekara.rpg.skills.experience.ExperienceGrantGuard;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class NativeActivityListener implements Listener {
    private static final int MAX_FINGERPRINTS = 65_536;
    private static final long BREWING_ATTRIBUTION_MILLIS = 120_000L;
    private static final int GRASS_BLOCKS_PER_AWARD = 8;

    private final NekaraRPGPlugin plugin;
    private final SkillsModule module;
    private final NativeActivityConfig config;
    private final PlacedBlockTracker placedBlocks;
    private final ExperienceGrantGuard guard;
    private final Map<BlockKey, BrewingActor> brewingActors = new HashMap<>();
    private final Map<UUID, Integer> grassForageProgress = new HashMap<>();
    private boolean enabled;

    NativeActivityListener(
        NekaraRPGPlugin plugin,
        SkillsModule module,
        NativeActivityConfig config,
        PlacedBlockTracker placedBlocks
    ) {
        this.plugin = plugin;
        this.module = module;
        this.config = config;
        this.placedBlocks = placedBlocks;
        this.guard = new ExperienceGrantGuard(
            Duration.ofMillis(config.deduplicationMillis()), MAX_FINGERPRINTS);
    }

    void enable() {
        if (enabled || !config.enabled()) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
    }

    void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        HandlerList.unregisterAll(this);
        brewingActors.clear();
        grassForageProgress.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (SyntheticCombatGuard.isActive() || event.getFinalDamage() <= 0.0) {
            return;
        }
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker != null && event.getEntity() instanceof Enemy target) {
            SkillId skill = event.getDamager() instanceof Projectile
                ? SkillId.ARCHERY
                : SkillEquipmentPolicy.meleeSkill(attacker.getInventory().getItemInMainHand()).orElse(null);
            if (skill != null) {
                award(attacker, skill, "combat_hit", target.getUniqueId().toString());
            }
        }

        if (event.getEntity() instanceof Player defender && isHostileSource(event.getDamager())) {
            SkillEquipmentPolicy.armorSkill(defender.getInventory()).ifPresent(skill ->
                award(defender, skill, "armor_hit", hostileSourceId(event.getDamager())));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(PlayerTradeEvent event) {
        award(event.getPlayer(), SkillId.TRADING, "villager_trade",
            event.getMerchant().getUniqueId() + ":" + event.getTrade().getUses());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
            || !SkillEquipmentPolicy.isSmithingProduct(event.getRecipe().getResult())) {
            return;
        }
        award(player, SkillId.SMITHING, "equipment_craft",
            event.getRecipe().getResult().getType().getKey() + ":" + Bukkit.getCurrentTick());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmith(SmithItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
            || !SkillEquipmentPolicy.isSmithingProduct(event.getCurrentItem())) {
            return;
        }
        award(player, SkillId.SMITHING, "smithing_table",
            event.getCurrentItem().getType().getKey() + ":" + Bukkit.getCurrentTick());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        award(event.getEnchanter(), SkillId.ENCHANTING, "enchant_item",
            event.getEnchantBlock().getWorld().getUID() + ":" + event.getEnchantBlock().getX() + ":"
                + event.getEnchantBlock().getY() + ":" + event.getEnchantBlock().getZ() + ":"
                + Bukkit.getCurrentTick());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void rememberBrewer(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
            || event.getView().getTopInventory().getType() != InventoryType.BREWING
            || !(event.getView().getTopInventory().getHolder() instanceof BrewingStand stand)) {
            return;
        }
        brewingActors.put(BlockKey.of(stand.getBlock()),
            new BrewingActor(player.getUniqueId(), System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        BrewingActor actor = brewingActors.remove(BlockKey.of(event.getBlock()));
        if (actor == null || System.currentTimeMillis() - actor.recordedAt() > BREWING_ATTRIBUTION_MILLIS) {
            return;
        }
        Player player = Bukkit.getPlayer(actor.playerId());
        if (player != null && player.isOnline()) {
            award(player, SkillId.ALCHEMY, "brew_complete",
                BlockKey.of(event.getBlock()) + ":" + Bukkit.getCurrentTick());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFarm(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (isHarvestable(block)) {
            award(event.getPlayer(), SkillId.FARMING, "mature_harvest",
                BlockKey.of(block) + ":" + block.getType().getKey());
            return;
        }
        String source = wildForageSource(block.getType().name());
        if (source == null || placedBlocks.isPlayerPlaced(block)
            || source.equals("grass_bundle") && !completedGrassBundle(event.getPlayer())) {
            return;
        }
        award(event.getPlayer(), SkillId.FARMING, source,
            BlockKey.of(block) + ":" + block.getType().getKey());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBerryHarvest(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (isBerryHarvestable(block)) {
            award(event.getPlayer(), SkillId.FARMING, "berry_harvest",
                BlockKey.of(block) + ":" + block.getType().getKey());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            award(player, SkillId.FARMING, "animal_breed", event.getEntity().getUniqueId().toString());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        award(event.getPlayer(), SkillId.FARMING, "animal_shear", event.getEntity().getUniqueId().toString());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        Player player = event.getPlayer();
        if (player != null && !event.getBlocks().isEmpty()) {
            award(player, SkillId.FARMING, "bonemeal_growth",
                BlockKey.of(event.getBlock()) + ":" + Bukkit.getCurrentTick());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        grassForageProgress.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanillaFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
            || !(event.getCaught() instanceof Item caught)) {
            return;
        }
        award(event.getPlayer(), SkillId.FISHING, "vanilla_catch", caught.getUniqueId().toString());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeliveredFish(FishingCatchDeliveredEvent event) {
        award(event.player(), SkillId.FISHING, "deferred_catch", event.catchId().toString());
    }

    private void award(Player player, SkillId skill, String sourceType, String sourceKey) {
        long baseExperience = Math.max(0L, Math.round(config.experience(skill, sourceType)
            * module.runtimeState(player.getUniqueId(), skill)
                .map(state -> state.stats().value(cz.nekara.rpg.skills.stats.StatId.EXPERIENCE_MULTIPLIER))
                .orElse(1.0)));
        if (!enabled || baseExperience < 1 || unsupportedMode(player)
            || !plugin.configuration().get().worlds().isEnabled(player.getWorld().getName())) {
            return;
        }
        ExperienceFingerprint fingerprint = new ExperienceFingerprint(
            player.getUniqueId().toString(), skill, sourceType, sourceKey);
        if (!guard.tryAcquire(fingerprint)) {
            return;
        }
        ExperienceContext context = new ExperienceContext(
            skill, false, false, false, false, false, false, 0);
        module.awardExperience(player.getUniqueId(), new ExperienceAwardRequest(
            player.getUniqueId().toString(), skill, baseExperience, context, fingerprint),
            result -> module.showExperienceFeedback(player.getUniqueId(), skill,
                ExperienceSourcePresentation.activity(sourceType), result));
    }

    private static Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static boolean isHostileSource(Entity damager) {
        return damager instanceof Enemy
            || damager instanceof Projectile projectile && projectile.getShooter() instanceof Enemy;
    }

    private static String hostileSourceId(Entity damager) {
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return shooter.getUniqueId().toString();
        }
        return damager.getUniqueId().toString();
    }

    private static boolean unsupportedMode(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }

    private static boolean isHarvestable(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    private boolean completedGrassBundle(Player player) {
        int progress = grassForageProgress.merge(player.getUniqueId(), 1, Integer::sum);
        if (progress < GRASS_BLOCKS_PER_AWARD) {
            return false;
        }
        grassForageProgress.put(player.getUniqueId(), 0);
        return true;
    }

    static String wildForageSource(String materialName) {
        return switch (materialName) {
            case "DANDELION", "POPPY", "BLUE_ORCHID", "ALLIUM", "AZURE_BLUET",
                "RED_TULIP", "ORANGE_TULIP", "WHITE_TULIP", "PINK_TULIP", "OXEYE_DAISY",
                "CORNFLOWER", "LILY_OF_THE_VALLEY", "WITHER_ROSE", "TORCHFLOWER",
                "PITCHER_PLANT", "OPEN_EYEBLOSSOM", "CLOSED_EYEBLOSSOM", "PINK_PETALS",
                "WILDFLOWERS" -> "wild_flower";
            case "BROWN_MUSHROOM", "RED_MUSHROOM", "CRIMSON_FUNGUS", "WARPED_FUNGUS" -> "wild_mushroom";
            case "SHORT_GRASS", "GRASS", "TALL_GRASS", "FERN", "LARGE_FERN" -> "grass_bundle";
            default -> null;
        };
    }

    private static boolean isBerryHarvestable(Block block) {
        String type = block.getType().name();
        if (type.equals("SWEET_BERRY_BUSH") && block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() > 1;
        }
        return (type.equals("CAVE_VINES") || type.equals("CAVE_VINES_PLANT"))
            && block.getBlockData().getAsString().contains("berries=true");
    }

    private record BrewingActor(UUID playerId, long recordedAt) {
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }
}
