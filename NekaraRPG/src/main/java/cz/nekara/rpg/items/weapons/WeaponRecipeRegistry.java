package cz.nekara.rpg.items.weapons;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.inventory.ShapedRecipe;

public final class WeaponRecipeRegistry {
    private final Server server;
    private final WeaponFactory factory;
    private final List<NamespacedKey> keys = new ArrayList<>();

    public WeaponRecipeRegistry(Server server) {
        this.server = server;
        this.factory = new WeaponFactory();
    }

    public void register() {
        if (!keys.isEmpty()) {
            return;
        }
        for (WeaponFamily family : WeaponFamily.values()) {
            if (!family.custom()) continue;
            for (WeaponTier tier : WeaponTier.values()) {
                WeaponDefinition definition = WeaponCatalog.custom(family, tier).orElseThrow();
                NamespacedKey key = new NamespacedKey("nekararpg", "weapons/" + definition.id());
                ShapedRecipe recipe = new ShapedRecipe(key, factory.create(definition));
                switch (family) {
                    case DAGGER -> recipe.shape(" A ", " S ");
                    case GREATSWORD -> recipe.shape("AAA", " B ", " S ");
                    case HAMMER -> recipe.shape("AAA", " S ", " S ");
                    default -> throw new IllegalStateException("Unexpected custom weapon family: " + family);
                }
                recipe.setIngredient('A', tier.craftingIngredient());
                recipe.setIngredient('S', org.bukkit.Material.STICK);
                if (family == WeaponFamily.GREATSWORD) {
                    recipe.setIngredient('B', tier.craftingIngredient());
                }
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
    }
}
