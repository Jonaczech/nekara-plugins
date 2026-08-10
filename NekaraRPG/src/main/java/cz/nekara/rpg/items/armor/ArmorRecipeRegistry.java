package cz.nekara.rpg.items.armor;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.Plugin;

/** Registers Nekara's armor upgrades without replacing vanilla recipes. */
public final class ArmorRecipeRegistry {
    private final Server server;
    private final List<NamespacedKey> keys = new ArrayList<>();

    public ArmorRecipeRegistry(Plugin plugin) {
        this.server = plugin.getServer();
    }

    public void register() {
        if (!keys.isEmpty()) {
            return;
        }
        for (ChainmailUpgrade upgrade : ChainmailUpgrade.values()) {
            NamespacedKey key = new NamespacedKey("nekararpg", "armor/chainmail_" + upgrade.id());
            SmithingTransformRecipe recipe = new SmithingTransformRecipe(
                key,
                new ItemStack(upgrade.chainmailResult()),
                RecipeChoice.empty(),
                new RecipeChoice.MaterialChoice(upgrade.leatherBase()),
                new RecipeChoice.MaterialChoice(Material.IRON_INGOT)
            );
            if (!server.addRecipe(recipe)) {
                unregister();
                throw new IllegalStateException("Could not register chainmail armor recipe " + key);
            }
            keys.add(key);
        }
    }

    public void unregister() {
        keys.forEach(server::removeRecipe);
        keys.clear();
    }
}
