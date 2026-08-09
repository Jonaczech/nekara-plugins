package cz.nekara.rpg.items.weapons;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.Plugin;

public final class WeaponRecipeRegistry implements Listener {
    private final Plugin plugin;
    private final Server server;
    private final WeaponFactory factory;
    private final List<NamespacedKey> keys = new ArrayList<>();

    public WeaponRecipeRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.factory = new WeaponFactory();
    }

    public void register() {
        if (!keys.isEmpty()) {
            return;
        }
        server.getPluginManager().registerEvents(this, plugin);
        for (WeaponFamily family : WeaponFamily.values()) {
            if (!family.custom()) continue;
            for (WeaponTier tier : WeaponTier.values()) {
                if (tier == WeaponTier.NETHERITE) continue;
                WeaponDefinition definition = WeaponCatalog.custom(family, tier).orElse(null);
                if (definition == null) continue;
                NamespacedKey key = new NamespacedKey("nekararpg", "weapons/" + definition.id());
                if (family == WeaponFamily.GREATSWORD) {
                    registerGreatswordSmithingRecipe(key, definition);
                    continue;
                }
                ShapedRecipe recipe = new ShapedRecipe(key, factory.create(definition));
                recipe.shape(WeaponRecipePattern.forFamily(family).rows());
                recipe.setIngredient('A', tier.craftingIngredient());
                recipe.setIngredient('S', org.bukkit.Material.STICK);
                if (!server.addRecipe(recipe)) {
                    unregister();
                    throw new IllegalStateException("Could not register weapon recipe " + key);
                }
                keys.add(key);
            }
        }
    }

    public void unregister() {
        keys.forEach(server::removeRecipe);
        keys.clear();
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void prepareNetheriteWeaponUpgrade(PrepareSmithingEvent event) {
        var inventory = event.getInventory();
        ItemStack template = inventory.getInputTemplate();
        ItemStack addition = inventory.getInputMineral();
        ItemStack base = inventory.getInputEquipment();
        if (template == null || template.getType() != Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE
            || addition == null || addition.getType() != Material.NETHERITE_INGOT) {
            return;
        }
        WeaponCatalog.resolve(base)
            .flatMap(WeaponCatalog::netheriteUpgrade)
            .map(factory::create)
            .ifPresent(event::setResult);
    }

    @EventHandler
    public void prepareGreatswordSmithing(PrepareSmithingEvent event) {
        var inventory = event.getInventory();
        GreatswordSmithingPolicy.result(
            typeOf(inventory.getInputTemplate()),
            typeOf(inventory.getInputEquipment()),
            typeOf(inventory.getInputMineral())
        ).map(WeaponTier::valueOf)
            .flatMap(tier -> WeaponCatalog.custom(WeaponFamily.GREATSWORD, tier))
            .map(factory::create)
            .ifPresent(event::setResult);
    }

    @EventHandler
    public void normalizeWeaponsOnJoin(PlayerJoinEvent event) {
        for (ItemStack item : event.getPlayer().getInventory().getContents()) {
            factory.normalizeAttributes(item);
        }
    }

    private void registerGreatswordSmithingRecipe(NamespacedKey key, WeaponDefinition definition) {
        WeaponTier tier = definition.tier();
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(
            key,
            factory.create(definition),
            RecipeChoice.empty(),
            new RecipeChoice.MaterialChoice(tier.vanillaMaterial("SWORD")),
            new RecipeChoice.MaterialChoice(tier.craftingIngredient())
        );
        if (!server.addRecipe(recipe)) {
            unregister();
            throw new IllegalStateException("Could not register smithing weapon recipe " + key);
        }
        keys.add(key);
    }

    @EventHandler
    public void normalizeHeldWeapon(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        factory.normalizeAttributes(item);
    }

    private static String typeOf(ItemStack item) {
        return item == null ? "AIR" : item.getType().name();
    }
}
