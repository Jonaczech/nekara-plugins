package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.perks.MechanicId;
import cz.nekara.rpg.skills.perks.PerkMechanicResolver;
import cz.nekara.rpg.skills.stats.PerkStatResolver;
import cz.nekara.rpg.skills.stats.StatId;
import org.bukkit.Keyed;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

final class GatheringUtilityPerkListener implements Listener {
    private final NekaraRPGPlugin plugin;
    private final SkillsModule module;
    private final PerkStatResolver stats;
    private final PerkMechanicResolver mechanics;
    private boolean enabled;

    GatheringUtilityPerkListener(
        NekaraRPGPlugin plugin,
        SkillsModule module,
        DefaultPerkTree perkTree
    ) {
        this.plugin = plugin;
        this.module = module;
        this.stats = new PerkStatResolver(perkTree.catalog());
        this.mechanics = new PerkMechanicResolver(perkTree.catalog());
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
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void speedUpViewedFurnace(FurnaceStartSmeltEvent event) {
        if (!enabled || !(event.getBlock().getState() instanceof Furnace furnace)
            || !plugin.configuration().get().worlds().isEnabled(event.getBlock().getWorld().getName())) {
            return;
        }
        double multiplier = 1.0;
        for (var viewer : furnace.getInventory().getViewers()) {
            if (!(viewer instanceof Player player)) {
                continue;
            }
            var profile = module.cachedProfile(player.getUniqueId());
            if (profile.isEmpty()) {
                continue;
            }
            try {
                multiplier = Math.max(multiplier,
                    stats.resolve(profile.get(), SkillId.MINING).value(StatId.FURNACE_SPEED));
            } catch (RuntimeException exception) {
                module.invalidateProfile(player.getUniqueId(), exception);
            }
        }
        if (multiplier <= 1.0) {
            return;
        }
        int perkCookTime = Math.max(1,
            (int) Math.ceil((double) event.getRecipe().getCookingTime() / multiplier));
        if (perkCookTime < event.getTotalCookTime()) {
            event.setTotalCookTime(perkCookTime);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void prepareEfficientPlanks(PrepareItemCraftEvent event) {
        if (!enabled || !(event.getRecipe() instanceof Keyed keyed)
            || !"minecraft".equals(keyed.getKey().getNamespace())) {
            return;
        }
        Player player = event.getViewers().stream()
            .filter(Player.class::isInstance)
            .map(Player.class::cast)
            .findFirst()
            .orElse(null);
        if (player == null) {
            return;
        }
        var profile = module.cachedProfile(player.getUniqueId());
        if (profile.isEmpty()) {
            return;
        }
        try {
            if (!mechanics.has(profile.get(), SkillId.WOODCUTTING, MechanicId.WOOD_RECIPES)) {
                return;
            }
        } catch (RuntimeException exception) {
            module.invalidateProfile(player.getUniqueId(), exception);
            return;
        }
        ItemStack result = event.getInventory().getResult();
        if (result == null || !result.getType().name().endsWith("_PLANKS") || result.getAmount() >= 5
            || !isSingleLogRecipe(event.getInventory().getMatrix())) {
            return;
        }
        ItemStack improved = result.clone();
        improved.setAmount(5);
        event.getInventory().setResult(improved);
    }

    private static boolean isSingleLogRecipe(ItemStack[] matrix) {
        int occupied = 0;
        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) {
                continue;
            }
            occupied++;
            if (!GatheringMaterialPolicy.isLog(item.getType())) {
                return false;
            }
        }
        return occupied == 1;
    }
}
