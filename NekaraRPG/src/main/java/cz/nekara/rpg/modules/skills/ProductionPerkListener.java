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
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

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
            improveQuality(event.getCurrentItem(), state.stats().value(StatId.ITEM_QUALITY));
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
            improveQuality(event.getCurrentItem(), state.stats().value(StatId.ITEM_QUALITY));
            refundCraftingIngredient(player, event.getInventory().getContents(),
                state.stats().value(StatId.RESOURCE_COST_REDUCTION));
        });
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

    private static void improveQuality(ItemStack item, double quality) {
        if (item == null || item.getType().isAir() || quality <= 1.0
            || ThreadLocalRandom.current().nextDouble() >= Math.min(1.0, quality - 1.0)) {
            return;
        }
        int current = item.getEnchantmentLevel(Enchantment.UNBREAKING);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, Math.min(4, current + 1));
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
