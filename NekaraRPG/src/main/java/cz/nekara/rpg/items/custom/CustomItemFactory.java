package cz.nekara.rpg.items.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomItemFactory {
    private final NamespacedKey itemIdKey;
    private final NamespacedKey schemaKey;
    private final NamespacedKey modelKey;

    public CustomItemFactory(JavaPlugin plugin) {
        itemIdKey = new NamespacedKey(plugin, "custom_item_id");
        schemaKey = new NamespacedKey(plugin, "custom_item_schema");
        modelKey = new NamespacedKey(plugin, "custom_item_model");
    }

    public ItemStack create(CustomItemDefinition definition) {
        ItemStack item = new ItemStack(definition.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(definition.displayName(), NamedTextColor.WHITE));
        meta.setItemModel(new NamespacedKey("nekararpg", definition.modelKey()));
        if (definition.customModelData() != null) {
            meta.setCustomModelData(definition.customModelData());
        }
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, definition.id());
        meta.getPersistentDataContainer().set(schemaKey, PersistentDataType.INTEGER,
                CustomItemDefinition.SCHEMA_VERSION);
        meta.getPersistentDataContainer().set(modelKey, PersistentDataType.STRING, definition.modelKey());

        EquipmentSlotGroup slot = equipmentSlot(definition.material().name());
        add(meta, Attribute.ATTACK_DAMAGE, "custom_item_attack_damage",
                definition.stats().attackDamage(), EquipmentSlotGroup.MAINHAND);
        add(meta, Attribute.ATTACK_SPEED, "custom_item_attack_speed",
                definition.stats().attackSpeed(), EquipmentSlotGroup.MAINHAND);
        add(meta, Attribute.ARMOR, "custom_item_armor", definition.stats().armor(), slot);
        add(meta, Attribute.ARMOR_TOUGHNESS, "custom_item_armor_toughness",
                definition.stats().armorToughness(), slot);
        add(meta, Attribute.MAX_HEALTH, "custom_item_max_health",
                definition.stats().maxHealthBonus(), slot);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("ID: " + definition.id(), NamedTextColor.DARK_GRAY));
        appendStat(lore, CustomItemStat.ATTACK_DAMAGE, definition.stats().attackDamage());
        appendStat(lore, CustomItemStat.ATTACK_SPEED, definition.stats().attackSpeed());
        appendStat(lore, CustomItemStat.ARMOR, definition.stats().armor());
        appendStat(lore, CustomItemStat.ARMOR_TOUGHNESS, definition.stats().armorToughness());
        appendStat(lore, CustomItemStat.MAX_HEALTH_BONUS, definition.stats().maxHealthBonus());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public String customItemId(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    private void add(ItemMeta meta, Attribute attribute, String key, Double value, EquipmentSlotGroup slot) {
        if (value == null) {
            return;
        }
        meta.addAttributeModifier(attribute, new AttributeModifier(
                new NamespacedKey("nekararpg", key), value, AttributeModifier.Operation.ADD_NUMBER, slot));
    }

    private EquipmentSlotGroup equipmentSlot(String material) {
        if (material.endsWith("_HELMET") || material.equals("TURTLE_HELMET")) {
            return EquipmentSlotGroup.HEAD;
        }
        if (material.endsWith("_CHESTPLATE") || material.equals("ELYTRA")) {
            return EquipmentSlotGroup.CHEST;
        }
        if (material.endsWith("_LEGGINGS")) {
            return EquipmentSlotGroup.LEGS;
        }
        if (material.endsWith("_BOOTS")) {
            return EquipmentSlotGroup.FEET;
        }
        return EquipmentSlotGroup.MAINHAND;
    }

    private void appendStat(List<Component> lore, CustomItemStat stat, Double value) {
        if (value != null) {
            lore.add(Component.text(stat.czechName() + ": " + format(value), NamedTextColor.GRAY));
        }
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
