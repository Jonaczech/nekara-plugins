package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.fishing.FishingCatchDeliveredEvent;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.combat.RandomChanceRoller;
import cz.nekara.rpg.skills.perks.MechanicId;
import cz.nekara.rpg.skills.rewards.DropMultiplierResolver;
import cz.nekara.rpg.skills.stats.StatId;
import io.papermc.paper.event.player.PlayerTradeEvent;
import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class ProductionPerkListener implements Listener {
    private static final long BREWING_ATTRIBUTION_MILLIS = 120_000L;
    private static final int MAX_FIELD_BLOCKS = 9;
    private static final long CROP_CARE_MILLIS = 600_000L;
    private static final int MAX_CARED_CHUNKS = 16_384;

    private final NekaraRPGPlugin plugin;
    private final SkillsModule module;
    private final SmithingTier.Keys smithingTierKeys;
    private final org.bukkit.NamespacedKey workshopKitKey;
    private final org.bukkit.NamespacedKey scoutArrowKey;
    private final Map<UUID, BukkitTask> sharpeningTasks = new HashMap<>();
    private final Map<BlockKey, BrewingActor> brewingActors = new HashMap<>();
    private final Map<ChunkKey, CropCaretaker> cropCaretakers = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ChunkKey, CropCaretaker> eldest) {
            return size() > MAX_CARED_CHUNKS;
        }
    };
    private final DropMultiplierResolver dropMultiplier = new DropMultiplierResolver();
    private boolean enabled;

    ProductionPerkListener(NekaraRPGPlugin plugin, SkillsModule module) {
        this.plugin = plugin;
        this.module = module;
        this.smithingTierKeys = SmithingTier.keys(plugin);
        this.workshopKitKey = new org.bukkit.NamespacedKey(plugin, "skills_workshop_kit");
        this.scoutArrowKey = new org.bukkit.NamespacedKey(plugin, "skills_scout_arrow");
    }

    void enable() {
        if (enabled) {
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
        cropCaretakers.clear();
        sharpeningTasks.values().forEach(BukkitTask::cancel);
        sharpeningTasks.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void applyTradePerks(PlayerTradeEvent event) {
        Player player = event.getPlayer();
        module.runtimeState(player.getUniqueId(), SkillId.TRADING).ifPresent(state -> {
            if (event.getMerchant() instanceof Villager villager) {
                Reputation reputation = villager.getReputation(player.getUniqueId());
                int current = reputation.getReputation(ReputationType.TRADING);
                int extra = Math.max(0, (int) Math.floor(
                    (state.stats().value(StatId.REPUTATION_GAIN) - 1.0) * 10.0));
                if (extra > 0) {
                    reputation.setReputation(ReputationType.TRADING, Math.min(25, current + extra));
                    villager.setReputation(player.getUniqueId(), reputation);
                }
                if (state.has(MechanicId.VILLAGER_TRAINING)) {
                    villager.setVillagerExperience(Math.min(Integer.MAX_VALUE - 1,
                        villager.getVillagerExperience() + 1));
                }
            }
            ItemStack price = event.getTrade().getIngredients().isEmpty()
                ? null : event.getTrade().getIngredients().getFirst();
            if (price != null && price.getType() == Material.EMERALD) {
                double expectedRefund = price.getAmount() * state.stats().value(StatId.TRADE_DISCOUNT);
                int refund = (int) expectedRefund;
                if (ThreadLocalRandom.current().nextDouble() < expectedRefund - refund) {
                    refund++;
                }
                if (refund > 0) {
                    giveLater(player, new ItemStack(Material.EMERALD, refund));
                }
            }
            double giftChance = state.has(MechanicId.VILLAGER_GIFTS) ? 0.05 : 0.0;
            if (state.has(MechanicId.BLACK_MARKET)) {
                giftChance += 0.02;
            }
            if (ThreadLocalRandom.current().nextDouble() < giftChance) {
                Material gift = state.has(MechanicId.BLACK_MARKET)
                    && ThreadLocalRandom.current().nextDouble() < 0.20
                    ? Material.AMETHYST_SHARD : Material.EMERALD;
                giveLater(player, new ItemStack(gift, 1));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void improveCraftedEquipment(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
            || !SkillEquipmentPolicy.isSmithingProduct(event.getCurrentItem())) {
            return;
        }
        module.runtimeState(player.getUniqueId(), SkillId.SMITHING).ifPresent(state -> {
            module.cachedProfile(player.getUniqueId()).ifPresent(profile -> SmithingTier.apply(
                event.getCurrentItem(), module.skillLevel(profile, SkillId.SMITHING),
                state.stats().value(StatId.ITEM_QUALITY), smithingTierKeys));
            refundCraftingIngredient(player, event.getInventory().getMatrix(),
                state.stats().value(StatId.RESOURCE_COST_REDUCTION));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void improveSmithingResult(SmithItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
            || !SkillEquipmentPolicy.isSmithingProduct(event.getCurrentItem())) {
            return;
        }
        module.runtimeState(player.getUniqueId(), SkillId.SMITHING).ifPresent(state -> {
            module.cachedProfile(player.getUniqueId()).ifPresent(profile -> SmithingTier.apply(
                event.getCurrentItem(), module.skillLevel(profile, SkillId.SMITHING),
                state.stats().value(StatId.ITEM_QUALITY), smithingTierKeys));
            refundCraftingIngredient(player, event.getInventory().getContents(),
                state.stats().value(StatId.RESOURCE_COST_REDUCTION));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void processCraftedEquipment(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK
            || event.getClickedBlock() == null || !supported(event.getPlayer())) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.configuration().get().worlds().isEnabled(player.getWorld().getName())) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        Material station = event.getClickedBlock().getType();
        if (station == Material.SMITHING_TABLE && player.isSneaking()
            && module.runtimeState(player.getUniqueId(), SkillId.SMITHING)
                .map(state -> state.has(MechanicId.TINKERING)).orElse(false)
            && repairWithWorkshopKit(player, item)) {
            event.setCancelled(true);
            return;
        }
        SmithingTier.ProcessingState state = SmithingTier.state(item, smithingTierKeys);
        if (station == Material.BLAST_FURNACE && state == SmithingTier.ProcessingState.UNPROCESSED) {
            if (!consumeFuel(player)) {
                player.sendActionBar(net.kyori.adventure.text.Component.text(
                    "Na nahřátí potřebuješ jeden kus uhlí.", net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }
            SmithingTier.advanceProcessing(item, smithingTierKeys,
                SmithingTier.ProcessingState.UNPROCESSED, SmithingTier.ProcessingState.HEATED);
            player.getWorld().spawnParticle(Particle.LAVA, event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5), 6,
                0.25, 0.25, 0.25, 0.0);
            player.playSound(player.getLocation(), Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 0.7F, 1.1F);
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                "Výkov je nahřátý. Ochlaď jej ve vodním kotli.", net.kyori.adventure.text.format.NamedTextColor.GOLD));
            event.setCancelled(true);
            return;
        }
        if (station == Material.WATER_CAULDRON && state == SmithingTier.ProcessingState.HEATED) {
            if (!SmithingTier.advanceProcessing(item, smithingTierKeys,
                SmithingTier.ProcessingState.HEATED, SmithingTier.ProcessingState.TEMPERED)) {
                return;
            }
            consumeCauldronWater(event.getClickedBlock());
            player.getWorld().spawnParticle(Particle.SPLASH, event.getClickedBlock().getLocation().add(0.5, 0.8, 0.5), 12,
                0.25, 0.1, 0.25, 0.08);
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.8F, 1.1F);
            String message = SmithingTier.isWeapon(item.getType())
                ? "Výkov je opracovaný. Naostři jej na brusu."
                : "Výkov je opracovaný a jeho ochrana je aktivní.";
            player.sendActionBar(net.kyori.adventure.text.Component.text(message,
                net.kyori.adventure.text.format.NamedTextColor.GREEN));
            event.setCancelled(true);
            return;
        }
        if (station == Material.GRINDSTONE && player.isSneaking()
            && SmithingTier.isWeapon(item.getType()) && state == SmithingTier.ProcessingState.TEMPERED) {
            startSharpening(player, event.getClickedBlock());
            event.setCancelled(true);
        }
    }

    private void startSharpening(Player player, Block grindstone) {
        if (sharpeningTasks.containsKey(player.getUniqueId())) {
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                "Ostření už probíhá.", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
            return;
        }
        Material weaponType = player.getInventory().getItemInMainHand().getType();
        org.bukkit.Location station = grindstone.getLocation().add(0.5, 0.5, 0.5);
        player.sendActionBar(net.kyori.adventure.text.Component.text(
            "Ostříš zbraň…", net.kyori.adventure.text.format.NamedTextColor.AQUA));
        player.getWorld().spawnParticle(Particle.CRIT, station, 8, 0.2, 0.2, 0.2, 0.05);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sharpeningTasks.remove(player.getUniqueId());
            ItemStack current = player.getInventory().getItemInMainHand();
            if (!player.isOnline() || !player.isSneaking() || current.getType() != weaponType
                || player.getLocation().distanceSquared(station) > 9.0
                || SmithingTier.state(current, smithingTierKeys) != SmithingTier.ProcessingState.TEMPERED) {
                if (player.isOnline()) {
                    player.sendActionBar(net.kyori.adventure.text.Component.text(
                        "Ostření bylo přerušeno.", net.kyori.adventure.text.format.NamedTextColor.RED));
                }
                return;
            }
            if (SmithingTier.advanceProcessing(current, smithingTierKeys,
                SmithingTier.ProcessingState.TEMPERED, SmithingTier.ProcessingState.SHARPENED)) {
                player.getWorld().spawnParticle(Particle.CRIT, station, 20, 0.25, 0.25, 0.25, 0.12);
                player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.9F, 1.1F);
                player.sendActionBar(net.kyori.adventure.text.Component.text(
                    "Zbraň je naostřená. Její Nekara damage je aktivní.",
                    net.kyori.adventure.text.format.NamedTextColor.GREEN));
            }
        }, 40L);
        sharpeningTasks.put(player.getUniqueId(), task);
    }

    private static boolean consumeFuel(Player player) {
        if (!player.getInventory().containsAtLeast(new ItemStack(Material.COAL), 1)) return false;
        player.getInventory().removeItem(new ItemStack(Material.COAL, 1));
        return true;
    }

    private static void consumeCauldronWater(Block cauldron) {
        if (!(cauldron.getBlockData() instanceof Levelled levelled)) return;
        int next = levelled.getLevel() - 1;
        if (next <= levelled.getMinimumLevel()) cauldron.setType(Material.CAULDRON, false);
        else {
            levelled.setLevel(next);
            cauldron.setBlockData(levelled, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void improveEnchantment(EnchantItemEvent event) {
        module.runtimeState(event.getEnchanter().getUniqueId(), SkillId.ENCHANTING).ifPresent(state -> {
            double costReduction = state.stats().value(StatId.EXPERIENCE_COST_REDUCTION);
            event.setExpLevelCost(Math.max(1,
                (int) Math.ceil(event.getExpLevelCost() * (1.0 - costReduction))));
            double power = state.stats().value(StatId.ENCHANTMENT_POWER);
            if (power > 1.0) {
                event.getEnchantsToAdd().replaceAll((enchantment, level) -> {
                    int improved = (int) Math.floor(level * power);
                    return Math.max(level, Math.min(enchantment.getMaxLevel() + 1, improved));
                });
            }
            double lapisSave = state.stats().value(StatId.RESOURCE_COST_REDUCTION);
            if (lapisSave > 0.0 && ThreadLocalRandom.current().nextDouble() < lapisSave
                && event.getInventory() instanceof EnchantingInventory enchanting) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack lapis = enchanting.getSecondary();
                    if (lapis == null || lapis.getType().isAir()) {
                        enchanting.setSecondary(new ItemStack(Material.LAPIS_LAZULI, 1));
                    } else if (lapis.getType() == Material.LAPIS_LAZULI
                        && lapis.getAmount() < lapis.getMaxStackSize()) {
                        lapis.setAmount(lapis.getAmount() + 1);
                    }
                });
            }
        });
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void accelerateBrewing(BrewingStartEvent event) {
        recentBrewer(event.getBlock()).flatMap(player ->
            module.runtimeState(player.getUniqueId(), SkillId.ALCHEMY)).ifPresent(state -> {
                double speed = state.stats().value(StatId.BREWING_SPEED);
                if (speed > 1.0) {
                    event.setBrewingTime(Math.max(20,
                        (int) Math.ceil(event.getBrewingTime() / speed)));
                }
            });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void improveBrew(BrewEvent event) {
        recentBrewer(event.getBlock()).ifPresent(player ->
            module.runtimeState(player.getUniqueId(), SkillId.ALCHEMY).ifPresent(state -> {
                double potionPower = state.stats().value(StatId.POTION_POWER);
                for (ItemStack result : event.getResults()) {
                    improvePotion(result, potionPower);
                }
                double saveChance = state.stats().value(StatId.RESOURCE_COST_REDUCTION);
                ItemStack ingredient = event.getContents().getIngredient();
                if (ingredient != null && !ingredient.getType().isAir()
                    && ThreadLocalRandom.current().nextDouble() < saveChance) {
                    giveLater(player, new ItemStack(ingredient.getType(), 1));
                }
            }));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void accelerateThrownPotion(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof ThrownPotion potion)
            || !(potion.getShooter() instanceof Player player)) {
            return;
        }
        module.runtimeState(player.getUniqueId(), SkillId.ALCHEMY).ifPresent(state -> {
            double speed = state.stats().value(StatId.THROWING_SPEED);
            if (speed > 1.0) {
                potion.setVelocity(potion.getVelocity().multiply(speed));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void harvestByHand(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
            || event.getClickedBlock() == null || !isMature(event.getClickedBlock())
            || !event.getPlayer().getInventory().getItemInMainHand().getType().name().endsWith("_HOE")) {
            return;
        }
        Player player = event.getPlayer();
        if (!supported(player)) {
            return;
        }
        Optional<SkillRuntimeState> state = module.runtimeState(player.getUniqueId(), SkillId.FARMING);
        if (state.isEmpty() || !state.get().has(MechanicId.INSTANT_HARVEST)) {
            return;
        }
        List<Block> harvest = new ArrayList<>();
        harvest.add(event.getClickedBlock());
        if (player.isSneaking() && state.get().has(MechanicId.FIELD_HARVEST)) {
            Material crop = event.getClickedBlock().getType();
            for (int x = -1; x <= 1 && harvest.size() < MAX_FIELD_BLOCKS; x++) {
                for (int z = -1; z <= 1 && harvest.size() < MAX_FIELD_BLOCKS; z++) {
                    Block candidate = event.getClickedBlock().getRelative(x, 0, z);
                    if (candidate.getType() == crop && isMature(candidate) && !harvest.contains(candidate)) {
                        harvest.add(candidate);
                    }
                }
            }
        }
        event.setCancelled(true);
        for (Block block : harvest) {
            replantAfterBreak(player, block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void rememberCropCaretaker(BlockBreakEvent event) {
        if (isMature(event.getBlock()) && supported(event.getPlayer())) {
            cropCaretakers.put(ChunkKey.of(event.getBlock()),
                new CropCaretaker(event.getPlayer().getUniqueId(), System.currentTimeMillis()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void accelerateCaredCrop(BlockGrowEvent event) {
        if (!(event.getNewState().getBlockData() instanceof Ageable ageable)
            || ageable.getAge() >= ageable.getMaximumAge()) {
            return;
        }
        CropCaretaker caretaker = cropCaretakers.get(ChunkKey.of(event.getBlock()));
        if (caretaker == null || System.currentTimeMillis() - caretaker.recordedAt() > CROP_CARE_MILLIS) {
            return;
        }
        module.runtimeState(caretaker.playerId(), SkillId.FARMING).ifPresent(state -> {
            double extraGrowthChance = Math.max(0.0,
                state.stats().value(StatId.CROP_GROWTH_MULTIPLIER) - 1.0);
            if (ThreadLocalRandom.current().nextDouble() < extraGrowthChance) {
                ageable.setAge(Math.min(ageable.getMaximumAge(), ageable.getAge() + 1));
                event.getNewState().setBlockData(ageable);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void improveBeekeepingYield(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
            || event.getClickedBlock() == null
            || !(event.getClickedBlock().getBlockData() instanceof Beehive hive)
            || hive.getHoneyLevel() < hive.getMaximumHoneyLevel()) {
            return;
        }
        Material held = event.getPlayer().getInventory().getItemInMainHand().getType();
        Material reward;
        if (held == Material.SHEARS) {
            reward = Material.HONEYCOMB;
        } else if (held == Material.GLASS_BOTTLE) {
            reward = Material.HONEY_BOTTLE;
        } else {
            return;
        }
        module.runtimeState(event.getPlayer().getUniqueId(), SkillId.FARMING).ifPresent(state -> {
            double extraYieldChance = Math.max(0.0,
                state.stats().value(StatId.BEEKEEPING_YIELD) - 1.0);
            if (ThreadLocalRandom.current().nextDouble() < extraYieldChance) {
                giveLater(event.getPlayer(), new ItemStack(reward, 1));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void multiplyHarvestDrops(BlockDropItemEvent event) {
        if (!isMature(event.getBlockState().getBlockData()) || event.getItems().isEmpty()) {
            return;
        }
        module.runtimeState(event.getPlayer().getUniqueId(), SkillId.FARMING).ifPresent(state -> {
            int multiplier = dropMultiplier.resolve(
                state.stats(), new RandomChanceRoller(ThreadLocalRandom.current()));
            if (multiplier <= 1) {
                return;
            }
            List<ItemStack> bonuses = new ArrayList<>();
            for (Item item : event.getItems()) {
                ItemStack original = item.getItemStack();
                for (int copy = 1; copy < multiplier; copy++) {
                    bonuses.add(original.clone());
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> bonuses.forEach(item ->
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(0.5, 0.5, 0.5), item)));
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void accelerateFishing(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.FISHING || !(event.getHook() instanceof FishHook hook)) {
            return;
        }
        module.runtimeState(event.getPlayer().getUniqueId(), SkillId.FISHING).ifPresent(state -> {
            double speed = state.stats().value(StatId.FISHING_SPEED);
            if (speed > 1.0) {
                hook.setMinWaitTime(Math.max(20, (int) Math.ceil(hook.getMinWaitTime() / speed)));
                hook.setMaxWaitTime(Math.max(hook.getMinWaitTime(),
                    (int) Math.ceil(hook.getMaxWaitTime() / speed)));
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void rewardVanillaFishingPerks(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof Item item) {
            rewardFishingPerks(event.getPlayer(), item.getItemStack(), event.getExpToDrop());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void rewardDeferredFishingPerks(FishingCatchDeliveredEvent event) {
        rewardFishingPerks(event.player(), event.catchItem(), event.vanillaExperience());
    }

    private void rewardFishingPerks(Player player, ItemStack catchItem, int vanillaExperience) {
        module.runtimeState(player.getUniqueId(), SkillId.FISHING).ifPresent(state -> {
            rewardFishingLevelTreasure(player);
            int extraExperience = (int) Math.floor(vanillaExperience
                * Math.max(0.0, state.stats().value(StatId.EXPERIENCE_ORB_MULTIPLIER) - 1.0));
            if (extraExperience > 0) {
                player.giveExp(extraExperience);
            }
            if (state.has(MechanicId.EQUIPMENT_FISHING)) {
                double chance = Math.min(0.08,
                    0.02 + state.stats().value(StatId.FISHING_LUCK) * 0.05);
                if (ThreadLocalRandom.current().nextDouble() < chance) {
                    Material[] equipment = {Material.FISHING_ROD, Material.BOW, Material.LEATHER_BOOTS};
                    giveLater(player, new ItemStack(equipment[ThreadLocalRandom.current().nextInt(equipment.length)]));
                }
            }
            if (state.has(MechanicId.EQUIPMENT_SALVAGING)
                && SkillEquipmentPolicy.isSmithingProduct(catchItem)) {
                giveLater(player, new ItemStack(Material.IRON_NUGGET,
                    ThreadLocalRandom.current().nextInt(1, 4)));
            }
        });
    }

    /**
     * Replaces the crafting preview before vanilla takes the result. This keeps
     * the amount visible in a crafting table and also makes shift-crafting use
     * the same output safely once per vanilla craft operation.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void previewBulkCraftingResult(PrepareItemCraftEvent event) {
        if (event.isRepair() || !(event.getView().getPlayer() instanceof Player player)
            || !supported(player) || !(event.getRecipe() instanceof org.bukkit.Keyed keyed)
            || !"minecraft".equals(keyed.getKey().getNamespace())
            || containsExcludedBulkIngredient(event.getInventory().getMatrix())) {
            return;
        }
        ItemStack vanillaResult = event.getRecipe().getResult();
        if (hasMechanic(player, SkillId.WOODCUTTING, MechanicId.WOOD_RECIPES)
            && isSingleLogRecipe(event.getInventory().getMatrix(), vanillaResult)) {
            ItemStack result = vanillaResult.clone();
            result.setAmount(5);
            event.getInventory().setResult(result);
            return;
        }
        if (vanillaResult.getType().isAir() || !isEfficientConstructionOutput(vanillaResult.getType())) {
            return;
        }
        module.runtimeState(player.getUniqueId(), SkillId.SMITHING).ifPresent(state -> {
            if (!state.has(MechanicId.BULK_CRAFTING)) return;
            module.cachedProfile(player.getUniqueId()).ifPresent(profile -> {
                ItemStack result = vanillaResult.clone();
                result.setAmount(SmithingTier.efficientOutput(vanillaResult.getAmount(),
                    module.skillLevel(profile, SkillId.SMITHING)));
                event.getInventory().setResult(result);
            });
        });
    }

    /**
     * Perk-only recipes intentionally remain in the vanilla crafting grid: they need no custom
     * blocks and vanilla consumes the displayed matrix only after the preview was authorized.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void previewPerkRecipeResult(PrepareItemCraftEvent event) {
        if (event.isRepair() || !(event.getView().getPlayer() instanceof Player player) || !supported(player)) {
            return;
        }
        ItemStack[] matrix = event.getInventory().getMatrix();
        if (hasMechanic(player, SkillId.SMITHING, MechanicId.SMITHING_RECIPES)
            && isWorkshopKitIngredients(matrix)) {
            event.getInventory().setResult(workshopKit());
        } else if (hasMechanic(player, SkillId.ALCHEMY, MechanicId.ALCHEMY_RECIPES)
            && isVitalityTonicIngredients(matrix)) {
            event.getInventory().setResult(vitalityTonic());
        } else if (hasMechanic(player, SkillId.ALCHEMY, MechanicId.POTION_MERGING)) {
            mergedPotion(matrix).ifPresent(event.getInventory()::setResult);
        } else if (hasMechanic(player, SkillId.ARCHERY, MechanicId.CUSTOM_ARROW_RECIPES)
            && isScoutArrowIngredients(matrix)) {
            event.getInventory().setResult(scoutArrows());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void preserveCraftedToolDurability(PlayerItemDamageEvent event) {
        Double chance = event.getItem().getPersistentDataContainer().get(
            smithingTierKeys.durabilitySave(), org.bukkit.persistence.PersistentDataType.DOUBLE);
        if (chance != null && chance > 0.0 && ThreadLocalRandom.current().nextDouble() < chance) {
            event.setCancelled(true);
        }
    }

    private boolean repairWithWorkshopKit(Player player, ItemStack item) {
        ItemStack kit = player.getInventory().getItemInOffHand();
        if (item == null || item.getType().isAir() || !(item.getItemMeta() instanceof Damageable damageable)
            || damageable.getDamage() <= 0 || kit == null || kit.getType() != Material.BUNDLE
            || !kit.getPersistentDataContainer().has(workshopKitKey, PersistentDataType.BYTE)) {
            return false;
        }
        int repair = Math.max(1, (int) Math.ceil(item.getType().getMaxDurability() * 0.25));
        damageable.setDamage(Math.max(0, damageable.getDamage() - repair));
        item.setItemMeta(damageable);
        kit.subtract(1);
        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 0.8F, 1.1F);
        player.sendActionBar(net.kyori.adventure.text.Component.text(
            "Řemeslnická souprava obnovila čtvrtinu odolnosti.",
            net.kyori.adventure.text.format.NamedTextColor.GREEN));
        return true;
    }

    private boolean hasMechanic(Player player, SkillId skill, MechanicId mechanic) {
        return module.runtimeState(player.getUniqueId(), skill).map(state -> state.has(mechanic)).orElse(false);
    }

    static boolean isWorkshopKitIngredients(ItemStack[] matrix) {
        return isWorkshopKitIngredients(materialGrid(matrix));
    }

    static boolean isWorkshopKitIngredients(Material[] matrix) {
        return hasExactly(matrix, Map.of(Material.IRON_NUGGET, 4, Material.PAPER, 1, Material.STRING, 1));
    }

    static boolean isScoutArrowIngredients(ItemStack[] matrix) {
        return isScoutArrowIngredients(materialGrid(matrix));
    }

    static boolean isScoutArrowIngredients(Material[] matrix) {
        return hasExactly(matrix, Map.of(Material.ARROW, 4, Material.GLOW_INK_SAC, 1, Material.AMETHYST_SHARD, 1));
    }

    private static boolean isVitalityTonicIngredients(ItemStack[] matrix) {
        if (!hasExactly(matrix, Map.of(Material.POTION, 1, Material.SWEET_BERRIES, 1, Material.GLOW_BERRIES, 1))) {
            return false;
        }
        return java.util.Arrays.stream(matrix)
            .filter(item -> item != null && item.getType() == Material.POTION)
            .allMatch(ProductionPerkListener::isWaterPotion);
    }

    private static boolean isWaterPotion(ItemStack item) {
        if (item == null || item.getType() != Material.POTION || !(item.getItemMeta() instanceof PotionMeta meta)) {
            return false;
        }
        return meta.getBasePotionType() == PotionType.WATER && meta.getAllEffects().isEmpty();
    }

    private static boolean hasExactly(ItemStack[] matrix, Map<Material, Integer> expected) {
        return hasExactly(materialGrid(matrix), expected);
    }

    private static boolean hasExactly(Material[] matrix, Map<Material, Integer> expected) {
        Map<Material, Integer> present = new HashMap<>();
        for (Material material : matrix) {
            if (material == null || material == Material.AIR) continue;
            present.merge(material, 1, Integer::sum);
        }
        return present.equals(expected);
    }

    private static Material[] materialGrid(ItemStack[] matrix) {
        Material[] materials = new Material[matrix.length];
        for (int index = 0; index < matrix.length; index++) {
            materials[index] = matrix[index] == null ? Material.AIR : matrix[index].getType();
        }
        return materials;
    }

    private static boolean isSingleLogRecipe(ItemStack[] matrix, ItemStack vanillaResult) {
        if (vanillaResult == null || !vanillaResult.getType().name().endsWith("_PLANKS")) {
            return false;
        }
        List<ItemStack> ingredients = java.util.Arrays.stream(matrix)
            .filter(item -> item != null && !item.getType().isAir())
            .toList();
        return ingredients.size() == 1 && isLogOrStem(ingredients.getFirst().getType());
    }

    private static boolean isLogOrStem(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
    }

    private ItemStack workshopKit() {
        ItemStack result = new ItemStack(Material.BUNDLE);
        org.bukkit.inventory.meta.ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(workshopKitKey, PersistentDataType.BYTE, (byte) 1);
        meta.displayName(net.kyori.adventure.text.Component.text("Řemeslnická souprava",
            net.kyori.adventure.text.format.NamedTextColor.GOLD));
        meta.lore(List.of(
            net.kyori.adventure.text.Component.text("Plížení + pravé tlačítko na smithing table", net.kyori.adventure.text.format.NamedTextColor.GRAY),
            net.kyori.adventure.text.Component.text("v hlavní ruce poškozená výbava, v levé souprava", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY),
            net.kyori.adventure.text.Component.text("Obnoví 25 % její odolnosti", net.kyori.adventure.text.format.NamedTextColor.GREEN)
        ));
        result.setItemMeta(meta);
        return result;
    }

    private static ItemStack vitalityTonic() {
        ItemStack result = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) result.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 0, false, true, true), true);
        meta.displayName(net.kyori.adventure.text.Component.text("Tonikum vitality",
            net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(net.kyori.adventure.text.Component.text(
            "Regeneration I na 45 sekund", net.kyori.adventure.text.format.NamedTextColor.GREEN)));
        result.setItemMeta(meta);
        return result;
    }

    private Optional<ItemStack> mergedPotion(ItemStack[] matrix) {
        if (!hasExactly(matrix, Map.of(Material.POTION, 2, Material.AMETHYST_SHARD, 1))) {
            return Optional.empty();
        }
        List<PotionEffect> effects = java.util.Arrays.stream(matrix)
            .filter(item -> item != null && item.getType() == Material.POTION)
            .flatMap(item -> ((PotionMeta) item.getItemMeta()).getAllEffects().stream())
            .toList();
        if (effects.isEmpty() || effects.stream().map(PotionEffect::getType).distinct().count() > 2) {
            return Optional.empty();
        }
        ItemStack result = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) result.getItemMeta();
        Map<PotionEffectType, PotionEffect> strongest = new HashMap<>();
        for (PotionEffect effect : effects) {
            strongest.merge(effect.getType(), effect, (left, right) -> left.getAmplifier() > right.getAmplifier()
                ? left : left.getAmplifier() < right.getAmplifier()
                    ? right : left.getDuration() >= right.getDuration() ? left : right);
        }
        strongest.values().forEach(effect -> meta.addCustomEffect(effect, true));
        meta.displayName(net.kyori.adventure.text.Component.text("Spojená esence",
            net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
        result.setItemMeta(meta);
        return Optional.of(result);
    }

    private ItemStack scoutArrows() {
        ItemStack result = new ItemStack(Material.ARROW, 4);
        org.bukkit.inventory.meta.ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(scoutArrowKey, PersistentDataType.BYTE, (byte) 1);
        meta.displayName(net.kyori.adventure.text.Component.text("Šíp průzkumníka",
            net.kyori.adventure.text.format.NamedTextColor.AQUA));
        meta.lore(List.of(net.kyori.adventure.text.Component.text(
            "Zásah označí cíl na 8 sekund", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        result.setItemMeta(meta);
        return result;
    }

    private void rewardFishingLevelTreasure(Player player) {
        var rewards = plugin.configuration().get().skills().fishingRewards();
        if (!rewards.treasureEnabled() || rewards.treasureWeights().isEmpty()) {
            return;
        }
        Optional<cz.nekara.rpg.skills.profile.SkillProfile> profile = module.cachedProfile(player.getUniqueId());
        if (profile.isEmpty()) {
            return;
        }
        int level = module.skillLevel(profile.get(), SkillId.FISHING);
        double chance = plugin.configuration().get().skills().levelRewards().fishingTreasureChance(level);
        if (chance <= 0.0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        selectWeighted(rewards.treasureWeights()).ifPresent(material ->
            giveLater(player, new ItemStack(material)));
    }

    private static Optional<Material> selectWeighted(Map<Material, Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return Optional.empty();
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (Map.Entry<Material, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private void replantAfterBreak(Player player, Block block) {
        if (!(block.getBlockData() instanceof Ageable original)) {
            return;
        }
        org.bukkit.block.data.BlockData replanted = original.clone();
        ((Ageable) replanted).setAge(0);
        BlockKey key = BlockKey.of(block);
        if (!player.breakBlock(block)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            Block current = key.block();
            if (enabled && current != null && current.getType().isAir()) {
                current.setBlockData(replanted, true);
            }
        });
    }

    private Optional<Player> recentBrewer(Block block) {
        BrewingActor actor = brewingActors.get(BlockKey.of(block));
        if (actor == null || System.currentTimeMillis() - actor.recordedAt() > BREWING_ATTRIBUTION_MILLIS) {
            return Optional.empty();
        }
        Player player = Bukkit.getPlayer(actor.playerId());
        return player != null && player.isOnline() ? Optional.of(player) : Optional.empty();
    }

    private void refundCraftingIngredient(Player player, ItemStack[] inputs, double chance) {
        if (chance <= 0.0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        List<ItemStack> candidates = java.util.Arrays.stream(inputs)
            .filter(item -> item != null && !item.getType().isAir())
            .map(ItemStack::clone)
            .toList();
        if (!candidates.isEmpty()) {
            ItemStack refund = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            refund.setAmount(1);
            giveLater(player, refund);
        }
    }

    static boolean isEfficientConstructionOutput(Material material) {
        if (isEfficientConstructionComponent(material)) {
            return true;
        }
        if (!material.isBlock()) return false;
        String name = material.name();
        if (name.endsWith("_ORE") || name.startsWith("RAW_") || name.endsWith("_CROP")
            || name.endsWith("_STEM") || name.endsWith("_BUSH") || name.endsWith("_SAPLING")) {
            return false;
        }
        return switch (material) {
            case CAKE, HAY_BLOCK, DRIED_KELP_BLOCK, MELON, PUMPKIN,
                COAL_BLOCK, IRON_BLOCK, GOLD_BLOCK, COPPER_BLOCK, DIAMOND_BLOCK,
                EMERALD_BLOCK, LAPIS_BLOCK, REDSTONE_BLOCK, NETHERITE_BLOCK,
                RAW_IRON_BLOCK, RAW_GOLD_BLOCK, RAW_COPPER_BLOCK -> false;
            default -> true;
        };
    }

    /** Non-block construction components deliberately covered by the bulk-crafting perk. */
    static boolean isEfficientConstructionComponent(Material material) {
        return switch (material) {
            case STICK, BOWL, CLAY_BALL, BRICK, NETHER_BRICK, RESIN_BRICK -> true;
            default -> false;
        };
    }

    private static boolean containsExcludedBulkIngredient(ItemStack[] ingredients) {
        for (ItemStack ingredient : ingredients) {
            if (ingredient != null && isCropOrMobDrop(ingredient.getType())) return true;
        }
        return false;
    }

    private static boolean isCropOrMobDrop(Material material) {
        String name = material.name();
        if (name.endsWith("_SEEDS") || name.endsWith("_SAPLING") || name.endsWith("_CROP")
            || name.endsWith("_BERRIES") || name.endsWith("_MUSHROOM") || name.endsWith("_FLOWER")) {
            return true;
        }
        return switch (material) {
            case WHEAT, CARROT, POTATO, BEETROOT, KELP, BAMBOO, SUGAR_CANE,
                MELON_SLICE, PUMPKIN, COCOA_BEANS, CACTUS, CHORUS_FRUIT,
                STRING, BONE, ROTTEN_FLESH, SPIDER_EYE, SLIME_BALL, LEATHER,
                FEATHER, RABBIT_HIDE, RABBIT_FOOT, GUNPOWDER, ENDER_PEARL,
                BLAZE_ROD, GHAST_TEAR, MAGMA_CREAM, SHULKER_SHELL,
                PHANTOM_MEMBRANE, INK_SAC, GLOW_INK_SAC, ARMADILLO_SCUTE -> true;
            default -> false;
        };
    }

    private static void improvePotion(ItemStack item, double power) {
        if (item == null || !(item.getItemMeta() instanceof PotionMeta meta) || power <= 1.0) {
            return;
        }
        PotionType base = meta.getBasePotionType();
        if (base != null && base.isUpgradeable()
            && ThreadLocalRandom.current().nextDouble() < Math.min(1.0, power - 1.0)) {
            try {
                meta.setBasePotionType(PotionType.valueOf("STRONG_" + base.name()));
            } catch (IllegalArgumentException ignored) {
                // Some potion families expose isUpgradeable without a STRONG_ enum variant.
            }
        }
        if (meta.hasCustomEffects()) {
            List<PotionEffect> effects = meta.getCustomEffects();
            meta.clearCustomEffects();
            for (PotionEffect effect : effects) {
                meta.addCustomEffect(new PotionEffect(
                    effect.getType(),
                    Math.max(1, (int) Math.round(effect.getDuration() * power)),
                    effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()), true);
            }
        }
        item.setItemMeta(meta);
    }

    private void giveLater(Player player, ItemStack item) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!enabled || !player.isOnline()) {
                return;
            }
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            leftovers.values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        });
    }

    private static boolean isMature(Block block) {
        return isMature(block.getBlockData());
    }

    private static boolean isMature(org.bukkit.block.data.BlockData data) {
        return data instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
    }

    private static boolean supported(Player player) {
        return player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
    }

    private record BrewingActor(UUID playerId, long recordedAt) {
    }

    private record CropCaretaker(UUID playerId, long recordedAt) {
    }

    private record ChunkKey(UUID worldId, int x, int z) {
        static ChunkKey of(Block block) {
            return new ChunkKey(block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ());
        }
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        Block block() {
            org.bukkit.World world = Bukkit.getWorld(worldId);
            return world == null || !world.isChunkLoaded(x >> 4, z >> 4)
                ? null : world.getBlockAt(x, y, z);
        }
    }
}
