package cz.nekara.rpg.items.weapons;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class WeaponFactory {
    private static final NamespacedKey DAMAGE_MODIFIER = new NamespacedKey("nekararpg", "weapon_attack_damage");
    private static final NamespacedKey SPEED_MODIFIER = new NamespacedKey("nekararpg", "weapon_attack_speed");

    public ItemStack create(WeaponDefinition definition) {
        if (!definition.custom()) {
            throw new IllegalArgumentException("Only custom weapons are created by the Nekara factory");
        }
        if (definition.tier() == WeaponTier.WOODEN) {
            throw new IllegalArgumentException("Wooden custom weapons are no longer available");
        }
        ItemStack item = new ItemStack(definition.material());
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(WeaponCatalog.WEAPON_ID_KEY, PersistentDataType.STRING, definition.id());
        meta.getPersistentDataContainer().set(WeaponCatalog.WEAPON_SCHEMA_KEY,
            PersistentDataType.INTEGER, WeaponCatalog.ITEM_SCHEMA_VERSION);
        meta.getPersistentDataContainer().set(WeaponCatalog.WEAPON_MODEL_KEY,
            PersistentDataType.STRING, definition.modelKey());
        meta.setItemModel(new NamespacedKey("nekararpg", definition.modelKey()));
        meta.displayName(Component.text(definition.tier().displayPrefix() + " " + definition.family().displayName(),
            definition.family().skill().name().equals("LIGHT_WEAPONS") ? NamedTextColor.AQUA : NamedTextColor.RED));
        meta.lore(List.of(
            Component.text(definition.family().skill() == cz.nekara.rpg.skills.SkillId.LIGHT_WEAPONS
                ? "Sek\u00e1n\u00ed a bod\u00e1n\u00ed" : "Brut\u00e1ln\u00ed boj", NamedTextColor.GRAY),
            Component.text(definition.family().requiresEmptyOffhand()
                ? "Vy\u017eaduje pr\u00e1zdnou druhou ruku" : "Zbra\u0148 Nekara", NamedTextColor.DARK_GRAY),
            Component.text("Typ po\u0161kozen\u00ed: " + definition.family().damageType().czechName(), NamedTextColor.GOLD),
            Component.text("Model: " + definition.modelKey(), NamedTextColor.DARK_GRAY)
        ));
        applyAttributes(meta, WeaponAttributes.forWeapon(definition));
        item.setItemMeta(meta);
        return item;
    }

    /** Applies Nekara's final combat attributes to a supported vanilla or custom weapon. */
    public boolean normalizeAttributes(ItemStack item) {
        WeaponDefinition definition = WeaponCatalog.resolve(item).orElse(null);
        if (definition == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        removeModifier(meta, Attribute.ATTACK_DAMAGE, DAMAGE_MODIFIER);
        removeModifier(meta, Attribute.ATTACK_SPEED, SPEED_MODIFIER);
        applyAttributes(meta, WeaponAttributes.forWeapon(definition));
        item.setItemMeta(meta);
        return true;
    }

    private static void applyAttributes(ItemMeta meta, WeaponAttributes attributes) {
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
            DAMAGE_MODIFIER, attributes.attackDamage(), AttributeModifier.Operation.ADD_NUMBER,
            EquipmentSlotGroup.MAINHAND));
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
            SPEED_MODIFIER, attributes.attackSpeed(), AttributeModifier.Operation.ADD_NUMBER,
            EquipmentSlotGroup.MAINHAND));
    }

    private static void removeModifier(ItemMeta meta, Attribute attribute, NamespacedKey key) {
        var modifiers = meta.getAttributeModifiers(attribute);
        if (modifiers == null) {
            return;
        }
        modifiers.stream().filter(modifier -> modifier.getKey().equals(key))
            .toList().forEach(modifier -> meta.removeAttributeModifier(attribute, modifier));
    }
}
