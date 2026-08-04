package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.perks.MechanicId;
import cz.nekara.rpg.skills.perks.PerkMechanicResolver;
import cz.nekara.rpg.skills.stats.PerkStatResolver;
import cz.nekara.rpg.skills.stats.StatId;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.StonecuttingRecipe;
import io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class GatheringUtilityPerkListener implements Listener {
    private final NekaraRPGPlugin plugin;
    private final SkillsModule module;
    private final PerkStatResolver stats;
    private final Map<NamespacedKey, WoodcutRecipe> woodcutRecipes = new HashMap<>();
    private boolean enabled;

    GatheringUtilityPerkListener(
        NekaraRPGPlugin plugin,
        SkillsModule module,
        DefaultPerkTree perkTree
    ) {
        this.plugin = plugin;
        this.module = module;
        this.stats = new PerkStatResolver(perkTree.catalog());
    }

    void enable() {
        if (enabled) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registerWoodcutRecipes();
        enabled = true;
    }

    void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        HandlerList.unregisterAll(this);
        woodcutRecipes.keySet().forEach(plugin.getServer()::removeRecipe);
        woodcutRecipes.clear();
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

    /** Shows the wood recipes in the normal stonecutter recipe list. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void improveSelectedWoodcutRecipe(PlayerStonecutterRecipeSelectEvent event) {
        if (!enabled) {
            return;
        }
        WoodcutRecipe recipe = woodcutRecipes.get(event.getStonecuttingRecipe().getKey());
        Player player = event.getPlayer();
        if (recipe == null || !plugin.configuration().get().worlds().isEnabled(player.getWorld().getName())) {
            return;
        }
        var profile = module.cachedProfile(player.getUniqueId());
        if (profile.isEmpty() || !module.runtimeState(player.getUniqueId(), SkillId.SMITHING)
            .map(state -> state.has(MechanicId.BULK_CRAFTING)).orElse(false)) {
            return;
        }
        ItemStack result = new ItemStack(recipe.result(),
            SmithingTier.efficientOutput(recipe.baseAmount(), module.skillLevel(profile.get(), SkillId.SMITHING)));
        StonecuttingRecipe upgraded = new StonecuttingRecipe(event.getStonecuttingRecipe().getKey(), result,
            new RecipeChoice.MaterialChoice(recipe.input()));
        upgraded.setGroup(event.getStonecuttingRecipe().getGroup());
        event.setStonecuttingRecipe(upgraded);
    }

    private void registerWoodcutRecipes() {
        registerWoodcutFamily("OAK", "LOG", "WOOD");
        registerWoodcutFamily("SPRUCE", "LOG", "WOOD");
        registerWoodcutFamily("BIRCH", "LOG", "WOOD");
        registerWoodcutFamily("JUNGLE", "LOG", "WOOD");
        registerWoodcutFamily("ACACIA", "LOG", "WOOD");
        registerWoodcutFamily("DARK_OAK", "LOG", "WOOD");
        registerWoodcutFamily("MANGROVE", "LOG", "WOOD");
        registerWoodcutFamily("CHERRY", "LOG", "WOOD");
        registerWoodcutFamily("PALE_OAK", "LOG", "WOOD");
        registerWoodcutFamily("CRIMSON", "STEM", "HYPHAE");
        registerWoodcutFamily("WARPED", "STEM", "HYPHAE");
        registerWoodcutInput("BAMBOO_BLOCK", "BAMBOO_PLANKS", 4);
        registerWoodcutInput("STRIPPED_BAMBOO_BLOCK", "BAMBOO_PLANKS", 4);
    }

    private void registerWoodcutFamily(String family, String primary, String secondary) {
        String planks = family + "_PLANKS";
        registerWoodcutInput(family + "_" + primary, planks, 4);
        registerWoodcutInput(family + "_" + secondary, planks, 4);
        registerWoodcutInput("STRIPPED_" + family + "_" + primary, planks, 4);
        registerWoodcutInput("STRIPPED_" + family + "_" + secondary, planks, 4);
        registerWoodcutInput(planks, family + "_SLAB", 2);
    }

    private void registerWoodcutInput(String inputName, String resultName, int baseAmount) {
        Material input = Material.matchMaterial(inputName);
        Material result = Material.matchMaterial(resultName);
        if (input == null || result == null) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "woodcut_" + inputName.toLowerCase(java.util.Locale.ROOT));
        StonecuttingRecipe recipe = new StonecuttingRecipe(key, new ItemStack(result, baseAmount), input);
        if (plugin.getServer().addRecipe(recipe)) {
            woodcutRecipes.put(key, new WoodcutRecipe(input, result, baseAmount));
        }
    }

    private record WoodcutRecipe(Material input, Material result, int baseAmount) { }

}
