package cz.nekara.rpg.modules.runes;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.menu.GuiItems;
import cz.nekara.rpg.modules.NekaraModule;
import cz.nekara.rpg.modules.skills.SkillsModule;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.perks.PerkId;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Lectern;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Owns the immutable rune item phases and their guarded workstation interactions. */
public final class RunesModule implements NekaraModule, Listener {
    public static final String ID = "runes";
    private static final int RUNE_SLOT = 4;
    private static final int DYE_INFO_SLOT = 10;
    private static final int TIER_I_SLOT = 12;
    private static final int TIER_II_SLOT = 13;
    private static final int TIER_III_SLOT = 14;
    private static final int CLOSE_SLOT = 22;
    private static final PerkId BASIC_RUNES = new PerkId("runotepectvi.runes");
    private static final PerkId ADVANCED_RUNES = new PerkId("runotepectvi.experience");
    private static final PerkId RUNE_MEMORY = new PerkId("runotepectvi.hexblade");
    private static final PerkId MASTER_RUNES = new PerkId("runotepectvi.limits");

    private final NekaraRPGPlugin plugin;
    private final SkillsModule skills;
    private final RuneItemFactory items;
    private final NamespacedKey blankRecipeKey;
    private final NamespacedKey trackerArrowKey;
    private final Map<UUID, ItemStack> awakeningRunes = new HashMap<>();
    private boolean enabled;

    public RunesModule(NekaraRPGPlugin plugin, SkillsModule skills) {
        this.plugin = plugin;
        this.skills = skills;
        this.items = new RuneItemFactory(plugin);
        this.blankRecipeKey = new NamespacedKey(plugin, "runes/blank");
        this.trackerArrowKey = new NamespacedKey(plugin, "runes/tracker_arrow");
    }

    @Override public String id() { return ID; }

    @Override
    public void enable() {
        if (enabled) return;
        Bukkit.addRecipe(blankRecipe());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
        Bukkit.getOnlinePlayers().forEach(RunesModule::refreshEquipmentLore);
    }

    @Override
    public void disable() {
        if (!enabled) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof AwakeningHolder) {
                player.closeInventory();
            }
        }
        returnStoredRunes(awakeningRunes);
        HandlerList.unregisterAll(this);
        Bukkit.removeRecipe(blankRecipeKey);
        enabled = false;
    }

    @Override public boolean isEnabled() { return enabled; }

    /** Creates a fresh custom blank rune for configured treasure rewards. */
    public ItemStack blankRune() { return items.blank(); }


    private ShapedRecipe blankRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(blankRecipeKey, items.blank());
        recipe.shape("CSC", "SRS", "CSC");
        recipe.setIngredient('C', Material.COAL);
        recipe.setIngredient('S', Material.SMOOTH_STONE);
        recipe.setIngredient('R', Material.REDSTONE);
        return recipe;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void awardBlankRuneCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player && items.isBlank(event.getCurrentItem())) {
            awardRuneExperience(player, "rune_craft");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void awardMagicalRuneImbuing(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() != 2
            || !(event.getView().getTopInventory() instanceof AnvilInventory inventory)
            || !single(inventory.getFirstItem()) || !items.isBlank(inventory.getFirstItem())
            || !single(inventory.getSecondItem()) || inventory.getSecondItem().getType() != Material.AMETHYST_SHARD) {
            return;
        }
        awardRuneExperience(player, "rune_imbue");
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void preserveEngravedRune(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() != 2
            || !(event.getView().getTopInventory() instanceof AnvilInventory inventory)
            || !skills.hasCachedPerk(player.getUniqueId(), RUNE_MEMORY)) return;
        ItemStack equipment = inventory.getFirstItem();
        ItemStack rune = inventory.getSecondItem();
        Optional<RuneEffect> effect = items.effect(rune);
        Optional<RuneTier> tier = items.tier(rune);
        if (!single(equipment) || !single(rune) || !items.isAwakened(rune) || effect.isEmpty() || tier.isEmpty()
            || !RuneSocketData.canEmbed(equipment, effect.get(), tier.get())) return;
        double returnChance = RunePolicy.engravingReturnChance(
            true, skills.hasCachedNewGamePlus(player.getUniqueId(), SkillId.ENCHANTING));
        if (ThreadLocalRandom.current().nextDouble() >= returnChance) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack remaining = inventory.getSecondItem();
            if (remaining == null || remaining.getType().isAir()) {
                give(player, items.awakened(effect.get(), tier.get()));
                int refund = RunePolicy.memoryExperienceRefund(tier.get());
                player.giveExpLevels(refund);
                player.sendActionBar(Component.text("Runová paměť vrátila runu a " + refund
                    + " úrovně XP.", NamedTextColor.AQUA));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void prepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack left = inventory.getFirstItem();
        ItemStack right = inventory.getSecondItem();
        if (single(left) && single(right) && items.isBlank(left) && right.getType() == Material.AMETHYST_SHARD) {
            event.setResult(items.magical());
            setAnvilCost(event, 3);
            return;
        }
        Optional<RuneEffect> effect = items.effect(right);
        Optional<RuneTier> tier = items.tier(right);
        if (single(left) && single(right) && items.isAwakened(right) && effect.isPresent() && tier.isPresent()
            && RuneSocketData.canEmbed(left, effect.get(), tier.get())) {
            event.setResult(items.embed(left, effect.get(), tier.get()));
            setAnvilCost(event, 5 + tier.get().value() * 2);
        }
    }

    private static void setAnvilCost(PrepareAnvilEvent event, int cost) {
        event.getView().setRepairCost(cost);
        event.getView().setMaximumRepairCost(cost);
        event.getView().setBypassCost(false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void openAwakening(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
            || event.getClickedBlock().getType() != Material.ENCHANTING_TABLE || event.getHand() == null
            || !items.isMagical(event.getItem())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (awakeningRunes.containsKey(player.getUniqueId())) return;
        ItemStack rune = takeOne(player, event.getHand());
        if (rune == null) return;
        awakeningRunes.put(player.getUniqueId(), rune);
        AwakeningHolder holder = new AwakeningHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27,
            Component.text("Runotepectví — Runa poznání", NamedTextColor.DARK_PURPLE));
        holder.inventory = inventory;
        player.openInventory(inventory);
        renderAwakeningMenu(player, inventory, rune);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.65F, 1.3F);
    }

    private void renderAwakeningMenu(Player player, Inventory inventory, ItemStack rune) {
        GuiItems.fill(inventory);
        inventory.setItem(RUNE_SLOT, rune.clone());
        int dyeCount = countMaterial(player, Material.WHITE_DYE);
        ItemStack dyeInfo = GuiItems.item(Material.WHITE_DYE,
            Component.text("Bílé barvivo: " + dyeCount + " ks", dyeCount > 0 ? NamedTextColor.WHITE : NamedTextColor.RED),
            Component.text("Barvivo zůstává v tvém inventáři.", NamedTextColor.GRAY),
            Component.text("Vybraný tier jej odebere automaticky.", NamedTextColor.DARK_GRAY));
        dyeInfo.setAmount(Math.max(1, Math.min(64, dyeCount)));
        inventory.setItem(DYE_INFO_SLOT, dyeInfo);
        inventory.setItem(TIER_I_SLOT, tierButton(player, RuneTier.I));
        inventory.setItem(TIER_II_SLOT, tierButton(player, RuneTier.II));
        inventory.setItem(TIER_III_SLOT, tierButton(player, RuneTier.III));
        inventory.setItem(CLOSE_SLOT, GuiItems.item(Material.BARRIER,
            Component.text("Zavřít", NamedTextColor.RED),
            Component.text("Magická runa se ti vrátí.", NamedTextColor.GRAY)));
    }

    private ItemStack tierButton(Player player, RuneTier tier) {
        boolean unlocked = canSelectTier(player, tier);
        int experienceCost = runeExperienceCost(player, tier);
        int dyeCost = RunePolicy.dyeCost(tier);
        boolean affordable = player.getLevel() >= experienceCost
            && countMaterial(player, Material.WHITE_DYE) >= dyeCost;
        NamedTextColor color = unlocked && affordable ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GRAY;
        Material icon = unlocked && affordable ? Material.ENCHANTED_BOOK : Material.BOOK;
        return GuiItems.item(icon,
            Component.text("Tier " + tier.name() + " — +" + RuneEffect.INSIGHT.value(tier) + " % XP", color),
            Component.text(tierRequirement(tier), NamedTextColor.DARK_GRAY),
            Component.text("Cena: " + experienceCost + " úrovní XP", NamedTextColor.GRAY),
            Component.text("Barvivo: " + dyeCost + "× bílé", NamedTextColor.GRAY),
            Component.text("Šance zachovat barvivo: " + dyePreservationPercent(player) + " %", NamedTextColor.GRAY),
            Component.text(unlocked && affordable ? "Klikni pro vytvoření" : "Nesplňuješ požadavky",
                unlocked && affordable ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void selectRuneTier(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
            || !(event.getView().getTopInventory().getHolder() instanceof AwakeningHolder holder)) return;
        event.setCancelled(true);
        if (!holder.owner.equals(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        RuneTier tier = tierForSlot(rawSlot);
        if (tier == null) return;
        ItemStack rune = awakeningRunes.get(player.getUniqueId());
        if (rune == null || !items.isMagical(rune)) {
            player.closeInventory();
            return;
        }
        if (!canSelectTier(player, tier)) {
            player.sendActionBar(Component.text(tierRequirement(tier), NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.7F);
            return;
        }
        int experienceCost = runeExperienceCost(player, tier);
        int dyeCost = RunePolicy.dyeCost(tier);
        if (player.getLevel() < experienceCost || countMaterial(player, Material.WHITE_DYE) < dyeCost) {
            player.sendActionBar(Component.text("Tier " + tier.name() + " vyžaduje " + experienceCost
                + " úrovní XP a " + dyeCost + "× bílé barvivo.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.7F);
            renderAwakeningMenu(player, event.getView().getTopInventory(), rune);
            return;
        }
        boolean preservedDye = RunePolicy.preservesDye(
            skills.cachedStat(player.getUniqueId(), SkillId.ENCHANTING, StatId.RUNE_DYE_PRESERVATION_CHANCE),
            ThreadLocalRandom.current().nextDouble());
        if (!preservedDye) {
            consumeMaterial(player, Material.WHITE_DYE, dyeCost);
        }
        player.setLevel(player.getLevel() - experienceCost);
        awakeningRunes.remove(player.getUniqueId());
        event.getView().getTopInventory().clear();
        RuneEffect effect = RuneEffect.INSIGHT;
        give(player, items.unstable(effect, tier));
        awardRuneExperience(player, "rune_inscribe");
        playEnchantingEffect(player);
        if (preservedDye) {
            player.sendActionBar(Component.text("Za hranou písma zachovalo použité barvivo.", NamedTextColor.AQUA));
        }
        player.closeInventory();
        player.sendMessage(Component.text("Vznikla nestabilní " + effect.displayName() + " " + tier.name()
            + ". Probuď ji kliknutím na prázdný lectern.", NamedTextColor.LIGHT_PURPLE));
    }

    private static RuneTier tierForSlot(int rawSlot) {
        return switch (rawSlot) {
            case TIER_I_SLOT -> RuneTier.I;
            case TIER_II_SLOT -> RuneTier.II;
            case TIER_III_SLOT -> RuneTier.III;
            default -> null;
        };
    }

    private boolean canSelectTier(Player player, RuneTier tier) {
        UUID playerId = player.getUniqueId();
        int level = skills.cachedSkillLevel(playerId, SkillId.ENCHANTING);
        int basicRank = skills.cachedPerkRank(playerId, BASIC_RUNES);
        int advancedRank = skills.cachedPerkRank(playerId, ADVANCED_RUNES);
        int masterRank = skills.cachedPerkRank(playerId, MASTER_RUNES);
        boolean available = RunePolicy.canSelectTier(tier, level, basicRank, advancedRank, masterRank);
        if (!available && level == 0 && basicRank == 0) skills.preloadProfile(player);
        return available;
    }

    private static String tierRequirement(RuneTier tier) {
        return switch (tier) {
            case I -> "Tier I vyžaduje Runotepectví 1 a perk Čitelné runy.";
            case II -> "Tier II vyžaduje Runotepectví 30 a III. hodnost perku Šetrný zápis.";
            case III -> "Tier III vyžaduje Runotepectví 70 a III. hodnost perku Za hranou písma.";
        };
    }

    private int runeExperienceCost(Player player, RuneTier tier) {
        double reduction = skills.cachedStat(player.getUniqueId(), SkillId.ENCHANTING,
            StatId.RUNE_EXPERIENCE_COST_REDUCTION);
        return RunePolicy.experienceCost(tier, reduction);
    }

    private int dyePreservationPercent(Player player) {
        return (int) Math.round(skills.cachedStat(player.getUniqueId(), SkillId.ENCHANTING,
            StatId.RUNE_DYE_PRESERVATION_CHANCE) * 100.0);
    }

    private static int countMaterial(Player player, Material material) {
        int amount = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == material) amount += item.getAmount();
        }
        return amount;
    }

    private static void consumeMaterial(Player player, Material material, int required) {
        int remaining = required;
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length && remaining > 0; slot++) {
            ItemStack item = storage[slot];
            if (item == null || item.getType() != material) continue;
            int consumed = Math.min(remaining, item.getAmount());
            if (consumed == item.getAmount()) player.getInventory().setItem(slot, null);
            else item.setAmount(item.getAmount() - consumed);
            remaining -= consumed;
        }
        if (remaining != 0) throw new IllegalStateException("Validated rune dye disappeared during selection");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void preventRuneDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AwakeningHolder) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void awakenAtLectern(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
            || event.getClickedBlock().getType() != Material.LECTERN || event.getHand() == null
            || !items.isUnstable(event.getItem())) return;
        if (!(event.getClickedBlock().getState() instanceof Lectern lectern) || !lectern.getInventory().isEmpty()) return;
        ItemStack held = event.getItem();
        Optional<RuneEffect> effect = items.effect(held);
        Optional<RuneTier> tier = items.tier(held);
        if (effect.isEmpty() || tier.isEmpty()) return;
        event.setCancelled(true);
        if (takeOne(event.getPlayer(), event.getHand()) == null) return;
        give(event.getPlayer(), items.awakened(effect.get(), tier.get()));
        awardRuneExperience(event.getPlayer(), "rune_awaken");
        playLecternEffect(event.getPlayer(), event.getClickedBlock());
        event.getPlayer().sendMessage(Component.text("Runa byla na lecternu probuzena.", NamedTextColor.AQUA));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void amplifyCollectedExperienceOrbs(PlayerExpChangeEvent event) {
        int base = event.getAmount();
        if (base < 1) return;
        double multiplier = skills.cachedStat(event.getPlayer().getUniqueId(), SkillId.ENCHANTING,
            StatId.EXPERIENCE_ORB_MULTIPLIER);
        int bonus = skills.claimSupplementalVanillaExperience(event.getPlayer().getUniqueId(),
            base * Math.max(0.0, multiplier - 1.0));
        if (bonus > 0) event.setAmount(base + bonus);
    }

    private void awardRuneExperience(Player player, String source) {
        skills.awardActivityExperience(player, SkillId.ENCHANTING, source,
            player.getUniqueId() + ":" + Bukkit.getCurrentTick());
    }
    private static void playEnchantingEffect(Player player) {
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0.0, 1.0, 0.0), 35, 0.8, 0.8, 0.8, 0.2);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.15F);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8F, 1.35F);
    }

    private static void playLecternEffect(Player player, org.bukkit.block.Block lectern) {
        var center = lectern.getLocation().add(0.5, 1.1, 0.5);
        lectern.getWorld().spawnParticle(Particle.ENCHANT, center, 45, 0.65, 0.45, 0.65, 0.25);
        lectern.getWorld().spawnParticle(Particle.PORTAL, center, 25, 0.35, 0.45, 0.35, 0.35);
        lectern.getWorld().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 0.8F);
        lectern.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.85F, 1.4F);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void closeRuneGui(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)
            || !(event.getView().getTopInventory().getHolder() instanceof AwakeningHolder)) return;
        ItemStack rune = awakeningRunes.remove(player.getUniqueId());
        if (rune != null) give(player, rune);
        event.getView().getTopInventory().clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void applyWeaponRune(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity target)) return;
        embedded(player.getInventory().getItemInMainHand(), RuneEffect.EMBER).ifPresent(tier ->
            target.setFireTicks(Math.max(target.getFireTicks(), tier.value() * 20)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void prepareTrackerArrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getShooter() instanceof Player player)) return;
        embedded(player.getInventory().getItemInMainHand(), RuneEffect.TRACKER).ifPresent(tier ->
            arrow.getPersistentDataContainer().set(trackerArrowKey, PersistentDataType.INTEGER, tier.value()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void applyTrackerArrow(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(event.getHitEntity() instanceof LivingEntity target)) return;
        Integer raw = arrow.getPersistentDataContainer().get(trackerArrowKey, PersistentDataType.INTEGER);
        if (raw != null) target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, RuneTier.fromValue(raw).value() * 80, 0, false, true, true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void applyBootRune(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) return;
        embedded(player.getInventory().getBoots(), RuneEffect.STEADFAST).ifPresent(tier ->
            event.setDamage(event.getDamage() * (1.0 - tier.value() * 0.10)));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void applyToolRune(PlayerItemDamageEvent event) {
        embedded(event.getItem(), RuneEffect.PRESERVATION).ifPresent(tier -> {
            if (Math.random() < tier.value() * 0.10) event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void refreshSocketLore(PlayerJoinEvent event) {
        refreshEquipmentLore(event.getPlayer());
    }

    private static void refreshEquipmentLore(Player player) {
        for (ItemStack item : player.getInventory().getContents()) RuneSocketData.refreshLore(item);
    }

    private Optional<RuneTier> embedded(ItemStack item, RuneEffect expected) {
        return RuneSocketData.firstTier(item, expected);
    }

    private static boolean single(ItemStack item) { return item != null && !item.getType().isAir() && item.getAmount() == 1; }

    private ItemStack takeOne(Player player, org.bukkit.inventory.EquipmentSlot hand) {
        ItemStack held = hand == org.bukkit.inventory.EquipmentSlot.HAND ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        if (held == null || held.getType().isAir()) return null;
        ItemStack result = held.clone(); result.setAmount(1);
        held.setAmount(held.getAmount() - 1);
        if (held.getAmount() <= 0) {
            if (hand == org.bukkit.inventory.EquipmentSlot.HAND) player.getInventory().setItemInMainHand(null);
            else player.getInventory().setItemInOffHand(null);
        }
        return result;
    }


    private static void give(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(value -> player.getWorld().dropItemNaturally(player.getLocation(), value));
    }

    private void returnStoredRunes(Map<UUID, ItemStack> stored) {
        stored.forEach((id, rune) -> { Player player = Bukkit.getPlayer(id); if (player != null) give(player, rune); });
        stored.clear();
    }

    private static final class AwakeningHolder implements InventoryHolder {
        private final UUID owner; private Inventory inventory;
        private AwakeningHolder(UUID owner) { this.owner = owner; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
