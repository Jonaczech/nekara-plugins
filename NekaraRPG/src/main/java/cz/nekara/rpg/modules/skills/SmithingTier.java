package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/** Original Nekara craft quality. Vanilla enchantments remain entirely separate. */
enum SmithingTier {
    I(0, "Tier I — Běžný výrobek", 0.0, 0.0, 0.0),
    II(20, "Tier II — Pevný výrobek", 0.25, 0.5, 0.10),
    III(40, "Tier III — Kvalitní výrobek", 0.5, 1.0, 0.20),
    IV(70, "Tier IV — Mistrovský výrobek", 0.75, 1.5, 0.30),
    V(100, "Tier V — Legendární výrobek", 1.0, 2.0, 0.40);

    private final int requiredLevel;
    private final String displayName;
    private final double weaponDamage;
    private final double armor;
    private final double durabilitySaveChance;

    SmithingTier(int requiredLevel, String displayName, double weaponDamage,
                 double armor, double durabilitySaveChance) {
        this.requiredLevel = requiredLevel;
        this.displayName = displayName;
        this.weaponDamage = weaponDamage;
        this.armor = armor;
        this.durabilitySaveChance = durabilitySaveChance;
    }

    static SmithingTier forLevel(int smithingLevel) {
        SmithingTier selected = I;
        for (SmithingTier tier : values()) {
            if (smithingLevel >= tier.requiredLevel) selected = tier;
        }
        return selected;
    }

    static Keys keys(NekaraRPGPlugin plugin) {
        return new Keys(
            new NamespacedKey(plugin, "smithing_tier"),
            new NamespacedKey(plugin, "smithing_weapon_damage"),
            new NamespacedKey(plugin, "smithing_armor"),
            new NamespacedKey(plugin, "smithing_durability_save"),
            new NamespacedKey(plugin, "smithing_processing_state")
        );
    }

    static boolean apply(ItemStack item, int smithingLevel, Keys keys) {
        return apply(item, smithingLevel, 1.0, keys);
    }

    static boolean apply(ItemStack item, int smithingLevel, double itemQuality, Keys keys) {
        if (item == null || item.getType().isAir() || !SkillEquipmentPolicy.isSmithingProduct(item)) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (data.has(keys.tier(), PersistentDataType.INTEGER)) return false;
        SmithingTier tier = forLevel(smithingLevel);
        if (tier != V && ThreadLocalRandom.current().nextDouble() < qualityPromotionChance(itemQuality)) {
            tier = values()[tier.ordinal() + 1];
        }
        data.set(keys.tier(), PersistentDataType.INTEGER, tier.ordinal() + 1);
        if (isWeapon(item.getType())) data.set(keys.weaponDamage(), PersistentDataType.DOUBLE, tier.weaponDamage);
        if (isArmor(item.getType())) data.set(keys.armor(), PersistentDataType.DOUBLE, tier.armor);
        if (isTool(item.getType())) data.set(keys.durabilitySave(), PersistentDataType.DOUBLE, tier.durabilitySaveChance);
        if (requiresProcessing(item.getType())) {
            data.set(keys.processingState(), PersistentDataType.BYTE, ProcessingState.UNPROCESSED.id());
        }
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Component.empty());
        lore.add(Component.text(tier.displayName, tier == V ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.AQUA));
        if (isWeapon(item.getType()) && tier.weaponDamage > 0) lore.add(Component.text("Nekara damage: +" + tier.weaponDamage, NamedTextColor.RED));
        if (isArmor(item.getType()) && tier.armor > 0) lore.add(Component.text("Nekara armor: +" + tier.armor, NamedTextColor.BLUE));
        if (isTool(item.getType()) && tier.durabilitySaveChance > 0) lore.add(Component.text("Úspora odolnosti: " + (int) (tier.durabilitySaveChance * 100) + "%", NamedTextColor.GREEN));
        if (requiresProcessing(item.getType())) {
            lore.addAll(processingLore(item.getType(), ProcessingState.UNPROCESSED));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    static double qualityPromotionChance(double itemQuality) {
        return Math.max(0.0, Math.min(1.0, itemQuality - 1.0));
    }

    static boolean advanceProcessing(ItemStack item, Keys keys, ProcessingState expected, ProcessingState next) {
        if (item == null || item.getType().isAir() || state(item, keys) != expected) return false;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keys.processingState(), PersistentDataType.BYTE, next.id());
        // The temporary glint makes a heated metal workpiece recognizable in an inventory.
        // Clearing the override later preserves the normal glint behaviour of real enchantments.
        meta.setEnchantmentGlintOverride(next == ProcessingState.HEATED ? true : null);
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.removeIf(component -> {
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
            return text.startsWith("Stav výkovu: ") || text.startsWith("Postup zpracování: ");
        });
        lore.addAll(processingLore(item.getType(), next));
        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    static ProcessingState state(ItemStack item, Keys keys) {
        if (item == null || item.getType().isAir()) return ProcessingState.NONE;
        Byte raw = item.getPersistentDataContainer().get(keys.processingState(), PersistentDataType.BYTE);
        return ProcessingState.fromId(raw);
    }

    static boolean armorBonusActive(ItemStack item, Keys keys) {
        ProcessingState state = state(item, keys);
        return state == ProcessingState.NONE || state == ProcessingState.TEMPERED;
    }

    static boolean weaponBonusActive(ItemStack item, Keys keys) {
        ProcessingState state = state(item, keys);
        return state == ProcessingState.NONE || state == ProcessingState.SHARPENED;
    }

    static int efficientOutput(int normalOutput, int smithingLevel) {
        if (normalOutput < 1) return normalOutput;
        int tierStep = forLevel(smithingLevel).ordinal();
        return Math.min(64, normalOutput + Math.max(0, (normalOutput * tierStep) / 4));
    }

    record Keys(NamespacedKey tier, NamespacedKey weaponDamage, NamespacedKey armor,
                NamespacedKey durabilitySave, NamespacedKey processingState) { }

    enum ProcessingState {
        NONE((byte) -1),
        UNPROCESSED((byte) 0),
        HEATED((byte) 1),
        TEMPERED((byte) 2),
        SHARPENED((byte) 3);

        private final byte id;

        ProcessingState(byte id) { this.id = id; }
        byte id() { return id; }

        static ProcessingState fromId(Byte id) {
            if (id == null) return NONE;
            for (ProcessingState value : values()) if (value.id == id) return value;
            return NONE;
        }
    }

    private static List<Component> processingLore(Material material, ProcessingState state) {
        boolean weapon = isWeapon(material);
        int totalSteps = weapon ? 4 : 3;
        int completeSteps = switch (state) {
            case UNPROCESSED -> 1;
            case HEATED -> 2;
            case TEMPERED -> 3;
            case SHARPENED -> 4;
            case NONE -> 0;
        };
        NamedTextColor color = switch (state) {
            case UNPROCESSED -> NamedTextColor.GRAY;
            case HEATED -> NamedTextColor.GOLD;
            case TEMPERED -> NamedTextColor.GREEN;
            case SHARPENED -> NamedTextColor.AQUA;
            case NONE -> NamedTextColor.DARK_GRAY;
        };
        String label = switch (state) {
            case UNPROCESSED -> "Nezpracovaný";
            case HEATED -> "Nahřátý";
            case TEMPERED -> weapon ? "Opracovaný — čeká ostření" : "Opracovaný — hotovo";
            case SHARPENED -> "Naostřený — hotovo";
            case NONE -> "Hotovo";
        };
        String nextStep = switch (state) {
            case UNPROCESSED -> "Další krok: Blast Furnace + uhlí";
            case HEATED -> "Další krok: vodní cauldron";
            case TEMPERED -> weapon ? "Další krok: plížení u grindstone" : "Ochrana Tieru je aktivní";
            case SHARPENED -> "Nekara damage je aktivní";
            case NONE -> "";
        };
        Component progress = Component.text("Postup zpracování: [", NamedTextColor.DARK_GRAY);
        for (int index = 0; index < totalSteps; index++) {
            progress = progress.append(Component.text(index < completeSteps ? "◆" : "◇",
                index < completeSteps ? color : NamedTextColor.DARK_GRAY));
        }
        progress = progress.append(Component.text("] " + nextStep, NamedTextColor.GRAY));
        return List.of(
            Component.text("Stav výkovu: " + label, color),
            progress
        );
    }

    static boolean isArmor(Material material) { String n = material.name(); return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS"); }
    static boolean isWeapon(Material material) { String n = material.name(); return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_SPEAR") || n.equals("MACE") || material == Material.TRIDENT || material == Material.BOW || material == Material.CROSSBOW; }
    static boolean isTool(Material material) { String n = material.name(); return n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE"); }

    /**
     * Only metal-like crafted equipment enters the workshop pipeline. Wooden,
     * stone and leather equipment retains its normal vanilla behaviour.
     */
    static boolean requiresProcessing(Material material) {
        if (!isArmor(material) && !isWeapon(material)) {
            return false;
        }
        String name = material.name();
        return !name.startsWith("WOODEN_")
            && !name.startsWith("STONE_")
            && !name.startsWith("LEATHER_")
            && material != Material.BOW
            && material != Material.CROSSBOW;
    }
}
