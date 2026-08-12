package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.items.weapons.WeaponCatalog;
import cz.nekara.rpg.items.weapons.WeaponFamily;
import cz.nekara.rpg.modules.runes.RuneSocketData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/** Original Nekara craft quality. Vanilla enchantments remain entirely separate. */
enum SmithingTier {
    I("Běžná", "◇", NamedTextColor.WHITE, false, 0.0, 0.0, 0.0),
    II("Neobyčejná", "✦", NamedTextColor.GREEN, false, 0.25, 0.5, 0.10),
    III("Vzácná", "◆", NamedTextColor.BLUE, false, 0.5, 1.0, 0.20),
    IV("Epická", "✹", NamedTextColor.LIGHT_PURPLE, true, 0.75, 1.5, 0.30),
    V("Legendární", "✪", NamedTextColor.GOLD, true, 1.0, 2.0, 0.40);

    private static final double LEGENDARY_ROLL_WEIGHT = 0.15;
    private static final double EPIC_ROLL_WEIGHT = 0.45;
    private static final double RARE_ROLL_WEIGHT = 0.80;

    private final String displayName;
    private final String icon;
    private final NamedTextColor displayColor;
    private final boolean bold;
    private final double weaponDamage;
    private final double armor;
    private final double durabilitySaveChance;

    SmithingTier(String displayName, String icon, NamedTextColor displayColor, boolean bold,
                 double weaponDamage,
                 double armor, double durabilitySaveChance) {
        this.displayName = displayName;
        this.icon = icon;
        this.displayColor = displayColor;
        this.bold = bold;
        this.weaponDamage = weaponDamage;
        this.armor = armor;
        this.durabilitySaveChance = durabilitySaveChance;
    }

    static Keys keys(NekaraRPGPlugin plugin) {
        return new Keys(
            new NamespacedKey(plugin, "smithing_tier"),
            new NamespacedKey(plugin, "smithing_weapon_damage"),
            new NamespacedKey(plugin, "smithing_armor"),
            new NamespacedKey(plugin, "smithing_durability_save"),
            new NamespacedKey(plugin, "smithing_processing_state"),
            new NamespacedKey(plugin, "smithing_quality_pending"),
            new NamespacedKey(plugin, "smithing_quality_chance"),
            new NamespacedKey(plugin, "smithing_quality_max_tier")
        );
    }

    static boolean prepare(
        ItemStack item,
        double qualityChance,
        int craftsmanshipRank,
        boolean fineWorkUnlocked,
        boolean masterworkUnlocked,
        Keys keys
    ) {
        if (item == null || item.getType().isAir() || !SkillEquipmentPolicy.isSmithingProduct(item)
            || isTailoringMaterial(item.getType())) return false;
        if (craftsmanshipRank < 1) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (data.has(keys.tier(), PersistentDataType.INTEGER)
            || data.has(keys.qualityPending(), PersistentDataType.BYTE)) return false;
        data.set(keys.qualityPending(), PersistentDataType.BYTE, (byte) 1);
        data.set(keys.qualityChance(), PersistentDataType.DOUBLE, Math.max(0.0, Math.min(1.0, qualityChance)));
        data.set(keys.qualityMaximum(), PersistentDataType.BYTE,
            (byte) (maximumQuality(craftsmanshipRank, fineWorkUnlocked, masterworkUnlocked).ordinal() + 1));
        if (requiresWorkshopProcessing(item)) {
            data.set(keys.processingState(), PersistentDataType.BYTE, ProcessingState.UNPROCESSED.id());
        }
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Component.empty());
        lore.add(Component.text("Kvalita: bude určena po dokončení výkovu.", NamedTextColor.GRAY));
        if (requiresWorkshopProcessing(item)) {
            lore.addAll(processingLore(item.getType(), ProcessingState.UNPROCESSED));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        EquipmentSkillLore.refresh(item);
        if (!requiresWorkshopProcessing(item)) {
            revealQuality(item, keys);
        }
        return true;
    }

    static Optional<SmithingTier> revealQuality(ItemStack item, Keys keys) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (!data.has(keys.qualityPending(), PersistentDataType.BYTE)
            || data.has(keys.tier(), PersistentDataType.INTEGER)) return Optional.empty();
        Double chance = data.get(keys.qualityChance(), PersistentDataType.DOUBLE);
        Byte maximumRaw = data.get(keys.qualityMaximum(), PersistentDataType.BYTE);
        SmithingTier maximum = fromStoredTier(maximumRaw);
        if (chance == null || maximum == null) return Optional.empty();
        SmithingTier tier = qualityFor(maximum, chance, ThreadLocalRandom.current().nextDouble());
        data.remove(keys.qualityPending());
        data.remove(keys.qualityChance());
        data.remove(keys.qualityMaximum());
        data.set(keys.tier(), PersistentDataType.INTEGER, tier.ordinal() + 1);
        if (isWeapon(item.getType())) data.set(keys.weaponDamage(), PersistentDataType.DOUBLE, tier.weaponDamage);
        if (isArmor(item.getType())) data.set(keys.armor(), PersistentDataType.DOUBLE, tier.armor);
        if (isTool(item.getType())) data.set(keys.durabilitySave(), PersistentDataType.DOUBLE, tier.durabilitySaveChance);
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.removeIf(component -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(component).startsWith("Kvalita: bude určena po dokončení výkovu."));
        lore.add(tier.qualityBadge());
        if (isWeapon(item.getType()) && tier.weaponDamage > 0) lore.add(Component.text("Bonusové poškození: +" + tier.weaponDamage, NamedTextColor.RED));
        if (isArmor(item.getType()) && tier.armor > 0) lore.add(Component.text("Bonusová obrana: +" + tier.armor, NamedTextColor.BLUE));
        if (isTool(item.getType()) && tier.durabilitySaveChance > 0) lore.add(Component.text("Úspora odolnosti: " + (int) (tier.durabilitySaveChance * 100) + "%", NamedTextColor.GREEN));
        meta.displayName(coloredName(meta, item, tier));
        meta.lore(lore);
        item.setItemMeta(meta);
        RuneSocketData.refreshLore(item);
        EquipmentSkillLore.refresh(item);
        return Optional.of(tier);
    }

    private static Component coloredName(ItemMeta meta, ItemStack item, SmithingTier tier) {
        Component current = meta.hasDisplayName()
            ? meta.displayName()
            : Component.translatable(item.getType().translationKey());
        return current.color(tier.displayColor).decoration(TextDecoration.BOLD, tier.bold);
    }
    static double qualityPromotionChance(double itemQuality) {
        return Math.max(0.0, Math.min(1.0, itemQuality - 1.0));
    }

    static double qualityPromotionChance(
        double itemQuality,
        double luckPoints,
        int maximumLuckPoints,
        double luckBonusPerPoint
    ) {
        double cappedLuck = Math.max(0.0, Math.min(luckPoints, Math.max(0, maximumLuckPoints)));
        return Math.max(0.0, Math.min(1.0,
            qualityPromotionChance(itemQuality) + cappedLuck * Math.max(0.0, luckBonusPerPoint)));
    }

    /**
     * Material access belongs to the Smithing level. Quality belongs only to
     * the dedicated craftsmanship perks. Once craftsmanship is unlocked,
     * crafted equipment is at least uncommon; higher tiers remain a roll.
     */
    static SmithingTier qualityFor(
        int craftsmanshipRank,
        boolean fineWorkUnlocked,
        boolean masterworkUnlocked,
        double itemQuality,
        double roll
    ) {
        if (craftsmanshipRank < 1) return I;
        SmithingTier maximum = maximumQuality(craftsmanshipRank, fineWorkUnlocked, masterworkUnlocked);
        return qualityFor(maximum, qualityPromotionChance(itemQuality), roll);
    }

    private static SmithingTier qualityFor(SmithingTier maximum, double chance, double roll) {
        if (maximum == V && roll < chance * LEGENDARY_ROLL_WEIGHT) return V;
        if (maximum.ordinal() >= IV.ordinal() && roll < chance * EPIC_ROLL_WEIGHT) return IV;
        if (maximum.ordinal() >= III.ordinal() && roll < chance * RARE_ROLL_WEIGHT) return III;
        return II;
    }

    private static SmithingTier maximumQuality(
        int craftsmanshipRank,
        boolean fineWorkUnlocked,
        boolean masterworkUnlocked
    ) {
        if (masterworkUnlocked) return V;
        if (fineWorkUnlocked) return IV;
        return craftsmanshipRank >= 3 ? III : II;
    }

    private static SmithingTier fromStoredTier(Byte raw) {
        if (raw == null || raw < 1 || raw > values().length) return null;
        return values()[raw - 1];
    }

    String displayName() {
        return displayName;
    }

    String icon() {
        return icon;
    }

    NamedTextColor displayColor() {
        return displayColor;
    }

    boolean bold() {
        return bold;
    }

    private Component qualityBadge() {
        return Component.text(icon + " " + displayName, displayColor)
            .decoration(TextDecoration.BOLD, bold);
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
            return text.startsWith("Stav výkovu: ") || text.startsWith("Postup zpracování: ")
                || text.startsWith("Výkov: ");
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
        int tierStep = productionStepForLevel(smithingLevel);
        return Math.min(64, normalOutput + Math.max(0, (normalOutput * tierStep) / 4));
    }

    private static int productionStepForLevel(int smithingLevel) {
        if (smithingLevel >= 100) return 4;
        if (smithingLevel >= 70) return 3;
        if (smithingLevel >= 40) return 2;
        if (smithingLevel >= 20) return 1;
        return 0;
    }

    record Keys(NamespacedKey tier, NamespacedKey weaponDamage, NamespacedKey armor,
                NamespacedKey durabilitySave, NamespacedKey processingState, NamespacedKey qualityPending,
                NamespacedKey qualityChance, NamespacedKey qualityMaximum) { }

    enum ProcessingState {
        NONE((byte) -1),
        UNPROCESSED((byte) 0),
        HEATED((byte) 1),
        TEMPERED((byte) 2),
        SHARPENED((byte) 3),
        // Keep the existing stored IDs stable for equipment already in player inventories.
        HAMMERED((byte) 4);

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
        int totalSteps = weapon ? 5 : 4;
        int completeSteps = switch (state) {
            case UNPROCESSED -> 1;
            case HEATED -> 2;
            case HAMMERED -> 3;
            case TEMPERED -> 4;
            case SHARPENED -> 5;
            case NONE -> 0;
        };
        NamedTextColor color = switch (state) {
            case UNPROCESSED -> NamedTextColor.GRAY;
            case HEATED -> NamedTextColor.GOLD;
            case HAMMERED -> NamedTextColor.YELLOW;
            case TEMPERED -> NamedTextColor.GREEN;
            case SHARPENED -> NamedTextColor.AQUA;
            case NONE -> NamedTextColor.DARK_GRAY;
        };
        String nextStep = switch (state) {
            case UNPROCESSED -> "Pec";
            case HEATED -> "Kovadlina";
            case HAMMERED -> "Kotlík";
            case TEMPERED -> weapon ? "Brus" : "Hotovo";
            case SHARPENED, NONE -> "Hotovo";
        };
        Component progress = Component.text("Výkov: ", NamedTextColor.DARK_GRAY);
        for (int index = 0; index < totalSteps; index++) {
            progress = progress.append(Component.text(index < completeSteps ? "◆" : "◇",
                index < completeSteps ? color : NamedTextColor.DARK_GRAY));
        }
        progress = progress.append(Component.text("  » " + nextStep, color));
        return List.of(progress);
    }
    static boolean isArmor(Material material) { String n = material.name(); return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS"); }
    static boolean isWeapon(Material material) { String n = material.name(); return n.endsWith("_SWORD") || n.endsWith("_AXE") || n.endsWith("_SPEAR") || n.equals("MACE") || material == Material.TRIDENT || material == Material.BOW || material == Material.CROSSBOW; }
    static boolean isTool(Material material) { String n = material.name(); return n.endsWith("_PICKAXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE"); }

    /**
     * Only metal-like crafted equipment enters the workshop pipeline. Wooden,
     * stone, leather and chainmail equipment retains its normal vanilla behaviour.
     */
    static boolean isWorkshopFurnace(Material material) {
        return material == Material.BLAST_FURNACE;
    }
    static boolean isTailoringMaterial(Material material) {
        String name = material.name();
        return name.startsWith("LEATHER_") || name.startsWith("CHAINMAIL_");
    }
    static boolean requiresCustomBlastFurnaceHeating(Material material) {
        return requiresProcessing(material)
            && (material.name().startsWith("DIAMOND_") || material.name().startsWith("NETHERITE_"));
    }

    private static boolean requiresWorkshopProcessing(ItemStack item) {
        return requiresProcessing(item.getType())
            && WeaponCatalog.resolve(item).map(definition -> definition.family() != WeaponFamily.HAMMER).orElse(true);
    }

    static boolean requiresProcessing(Material material) {
        if (!isArmor(material) && !isWeapon(material) && !isTool(material)) {
            return false;
        }
        String name = material.name();
        return !name.startsWith("WOODEN_")
            && !name.startsWith("STONE_")
            && !name.startsWith("LEATHER_")
            && !name.startsWith("CHAINMAIL_")
            && material != Material.BOW
            && material != Material.CROSSBOW;
    }
}
