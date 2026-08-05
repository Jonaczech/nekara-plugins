package cz.nekara.rpg.items.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class WeaponCatalog {
    public static final int ITEM_SCHEMA_VERSION = 1;
    public static final NamespacedKey WEAPON_ID_KEY = new NamespacedKey("nekararpg", "weapon_id");
    public static final NamespacedKey WEAPON_SCHEMA_KEY = new NamespacedKey("nekararpg", "weapon_schema");
    public static final NamespacedKey WEAPON_MODEL_KEY = new NamespacedKey("nekararpg", "weapon_model");

    private static final Map<String, WeaponDefinition> CUSTOM = definitions(true);

    private WeaponCatalog() {
    }

    public static Optional<WeaponDefinition> resolve(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        Integer schema = item.getPersistentDataContainer().get(WEAPON_SCHEMA_KEY, PersistentDataType.INTEGER);
        String id = item.getPersistentDataContainer().get(WEAPON_ID_KEY, PersistentDataType.STRING);
        if (schema != null || id != null) {
            return schema != null && schema == ITEM_SCHEMA_VERSION && id != null
                ? Optional.ofNullable(CUSTOM.get(id)) : Optional.empty();
        }
        return resolveVanilla(item.getType());
    }

    public static Optional<WeaponDefinition> custom(WeaponFamily family, WeaponTier tier) {
        if (!family.custom()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CUSTOM.get(new WeaponDefinition(family, tier, customBaseMaterial(family, tier)).id()));
    }

    public static boolean isCustomWeapon(ItemStack item) {
        return resolve(item).map(WeaponDefinition::custom).orElse(false);
    }

    public static Optional<WeaponDefinition> resolveVanilla(Material material) {
        for (WeaponTier tier : WeaponTier.values()) {
            if (material == tier.vanillaMaterial("SWORD")) return Optional.of(new WeaponDefinition(WeaponFamily.SWORD, tier, material));
            if (material == tier.vanillaMaterial("SPEAR")) return Optional.of(new WeaponDefinition(WeaponFamily.SPEAR, tier, material));
            if (material == tier.vanillaMaterial("AXE")) return Optional.of(new WeaponDefinition(WeaponFamily.AXE, tier, material));
        }
        return Optional.empty();
    }

    private static Map<String, WeaponDefinition> definitions(boolean customOnly) {
        Map<String, WeaponDefinition> definitions = new HashMap<>();
        for (WeaponFamily family : WeaponFamily.values()) {
            if (customOnly && !family.custom()) continue;
            for (WeaponTier tier : WeaponTier.values()) {
                WeaponDefinition definition = new WeaponDefinition(family, tier, customBaseMaterial(family, tier));
                definitions.put(definition.id(), definition);
            }
        }
        return Map.copyOf(definitions);
    }

    private static Material customBaseMaterial(WeaponFamily family, WeaponTier tier) {
        return switch (family) {
            case DAGGER, GREATSWORD -> tier.vanillaMaterial("SWORD");
            case HAMMER -> tier.vanillaMaterial("AXE");
            default -> throw new IllegalArgumentException("Only custom weapon families have a custom base material: " + family);
        };
    }
}
