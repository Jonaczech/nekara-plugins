package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.stats.StatId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

public final class PerkIconResolver {
    /**
     * Curated iconography for the shipped perk catalog. Keep this keyed by the stable perk ID:
     * a perk's visual identity must not change when its effect composition changes.
     */
    private static final Map<String, Material> CATALOG_ICONS = Map.ofEntries(
        entry("martial_arts.discipline", Material.MACE), entry("martial_arts.footwork", Material.RABBIT_FOOT),
        entry("martial_arts.held_punch", Material.LEATHER), entry("martial_arts.air_combo", Material.WIND_CHARGE),
        entry("martial_arts.grapple", Material.LEAD), entry("martial_arts.meditation", Material.AMETHYST_CLUSTER),
        entry("trading.reputation", Material.EMERALD), entry("trading.discount", Material.GOLD_NUGGET),
        entry("trading.selection", Material.WRITABLE_BOOK), entry("trading.ordering", Material.BOOK),
        entry("trading.services", Material.ANVIL), entry("trading.black_market", Material.END_CRYSTAL),
        entry("smithing.craft", Material.SMITHING_TABLE), entry("smithing.economy", Material.BUNDLE),
        entry("smithing.recipes", Material.CRAFTING_TABLE), entry("smithing.fine_work", Material.DIAMOND),
        entry("smithing.tinkering", Material.GRINDSTONE), entry("smithing.masterwork", Material.NETHER_STAR),
        entry("runotepectvi.runes", Material.ENCHANTED_BOOK), entry("runotepectvi.experience", Material.EXPERIENCE_BOTTLE),
        entry("runotepectvi.lapis", Material.LAPIS_LAZULI), entry("runotepectvi.insight", Material.SCULK_CATALYST),
        entry("runotepectvi.limits", Material.ENCHANTING_TABLE), entry("runotepectvi.hexblade", Material.CRYING_OBSIDIAN),
        entry("alchemy.potency", Material.POTION), entry("alchemy.brew_speed", Material.BREWING_STAND),
        entry("alchemy.ingredients", Material.NETHER_WART), entry("alchemy.vials", Material.SPLASH_POTION),
        entry("alchemy.recipes", Material.WRITTEN_BOOK), entry("alchemy.merging", Material.LINGERING_POTION),
        entry("tezba.yield", Material.IRON_PICKAXE), entry("tezba.tempo", Material.DIAMOND_PICKAXE),
        entry("tezba.furnace", Material.FURNACE), entry("tezba.vein", Material.DIAMOND_ORE),
        entry("tezba.blast", Material.TNT), entry("tezba.triple", Material.DEEPSLATE),
        entry("lesnictvi.yield", Material.STONE_AXE), entry("lesnictvi.tempo", Material.GOLDEN_AXE),
        entry("lesnictvi.recipes", Material.COOKED_BEEF), entry("lesnictvi.feller", Material.OAK_LOG),
        entry("lesnictvi.leaves", Material.OAK_LEAVES), entry("lesnictvi.triple", Material.AMETHYST_SHARD),
        entry("kopani.yield", Material.IRON_SHOVEL), entry("kopani.tempo", Material.DIAMOND_SHOVEL),
        entry("kopani.finds", Material.SNIFFER_EGG), entry("kopani.archaeology", Material.BRUSH),
        entry("kopani.deep_soil", Material.GRASS_BLOCK), entry("kopani.triple", Material.CHEST),
        entry("statkarstvi.yield", Material.WHEAT), entry("statkarstvi.growth", Material.BONE_MEAL),
        entry("statkarstvi.husbandry", Material.COW_SPAWN_EGG), entry("statkarstvi.instant", Material.GOLDEN_HOE),
        entry("statkarstvi.triple", Material.BEE_NEST), entry("statkarstvi.field", Material.HAY_BLOCK),
        entry("rybareni.luck", Material.HEART_OF_THE_SEA), entry("rybareni.speed", Material.FISHING_ROD),
        entry("rybareni.wisdom", Material.KNOWLEDGE_BOOK), entry("rybareni.equipment", Material.BARREL),
        entry("rybareni.salvage", Material.BUBBLE_CORAL), entry("rybareni.master", Material.NAUTILUS_SHELL),
        entry("lehke_zbrane.damage", Material.IRON_SWORD), entry("lehke_zbrane.critical", Material.DIAMOND_SWORD),
        entry("lehke_zbrane.parry", Material.SHIELD), entry("lehke_zbrane.coating", Material.REDSTONE),
        entry("lehke_zbrane.immunity", Material.FERMENTED_SPIDER_EYE), entry("lehke_zbrane.master", Material.NETHERITE_SWORD),
        entry("heavy_weapons.damage", Material.IRON_AXE), entry("heavy_weapons.power", Material.NETHERITE_AXE),
        entry("heavy_weapons.critical", Material.ARMOR_STAND), entry("heavy_weapons.penetration", Material.GOAT_HORN),
        entry("heavy_weapons.coating", Material.MAGMA_BLOCK), entry("heavy_weapons.master", Material.DRAGON_BREATH),
        entry("archery.damage", Material.BOW), entry("archery.accuracy", Material.TARGET),
        entry("archery.critical", Material.CROSSBOW), entry("archery.arrows", Material.ARROW),
        entry("archery.charged", Material.SPECTRAL_ARROW), entry("archery.master", Material.FIREWORK_ROCKET),
        entry("light_armor.armor", Material.LEATHER_CHESTPLATE), entry("light_armor.mobility", Material.CHAINMAIL_BOOTS),
        entry("light_armor.dodge", Material.FEATHER), entry("light_armor.sustenance", Material.LEATHER_LEGGINGS),
        entry("light_armor.adrenaline", Material.SUGAR), entry("light_armor.master", Material.TURTLE_HELMET),
        entry("heavy_armor.armor", Material.IRON_CHESTPLATE), entry("heavy_armor.burden", Material.IRON_BOOTS),
        entry("heavy_armor.recovery", Material.GLISTERING_MELON_SLICE), entry("heavy_armor.reflection", Material.PRISMARINE_CRYSTALS),
        entry("heavy_armor.rage", Material.BLAZE_POWDER), entry("heavy_armor.master", Material.NETHERITE_CHESTPLATE)
    );

    private PerkIconResolver() {
    }

    public static Material resolve(PerkDefinition perk) {
        Objects.requireNonNull(perk, "perk");
        Material catalogIcon = CATALOG_ICONS.get(perk.id().value());
        if (catalogIcon != null) {
            return catalogIcon;
        }
        return perk.effects().stream()
            .min(Comparator.comparingInt(PerkIconResolver::priority))
            .map(PerkIconResolver::material)
            .orElse(Material.BOOK);
    }

    static boolean hasCatalogIcon(PerkId perkId) {
        return CATALOG_ICONS.containsKey(Objects.requireNonNull(perkId, "perkId").value());
    }

    private static Map.Entry<String, Material> entry(String perkId, Material material) {
        return Map.entry(perkId, material);
    }

    private static int priority(PerkEffectDefinition effect) {
        if (effect instanceof StatPerkEffect stat) {
            return switch (stat.statId()) {
                case BLEED_CHANCE, BLEED_DAMAGE_MULTIPLIER -> 0;
                case STUN_CHANCE -> 1;
                case CRITICAL_CHANCE, CRITICAL_DAMAGE_MULTIPLIER -> 2;
                default -> 20;
            };
        }
        return 10;
    }

    private static Material material(PerkEffectDefinition effect) {
        if (effect instanceof StatPerkEffect stat) {
            return statMaterial(stat.statId());
        }
        if (effect instanceof MechanicPerkEffect mechanic) {
            return mechanicMaterial(mechanic.mechanicId());
        }
        return Material.BOOK;
    }

    private static Material statMaterial(StatId stat) {
        return switch (stat) {
            case DAMAGE_MULTIPLIER -> Material.IRON_SWORD;
            case CRITICAL_CHANCE, CRITICAL_DAMAGE_MULTIPLIER, CRITICAL_BLEED_CHANCE -> Material.NETHERITE_SWORD;
            case BLEED_CHANCE, BLEED_DAMAGE_MULTIPLIER, BLEED_FLAT_DAMAGE -> Material.REDSTONE;
            case LIGHT_WEAPON_ATTACK_SPEED -> Material.IRON_SWORD;
            case STUN_CHANCE -> Material.MACE;
            case DOUBLE_DROP_CHANCE -> Material.CHEST;
            case TRIPLE_DROP_CHANCE -> Material.ENDER_CHEST;
            case MINING_SPEED -> Material.GOLDEN_PICKAXE;
            case MINING_BLOCK_EXPERIENCE -> Material.EXPERIENCE_BOTTLE;
            case DRILLING_SPEED_MULTIPLIER, DRILLING_COOLDOWN_REDUCTION, TNT_BONUS_DROP_CHANCE -> Material.TNT;
            case WOODCUTTING_SPEED -> Material.GOLDEN_AXE;
            case WOODCUTTING_LOG_EXPERIENCE, WOODCUTTING_LEAF_EXPERIENCE -> Material.EXPERIENCE_BOTTLE;
            case GOLDEN_LEAF_APPLE_CHANCE -> Material.GOLDEN_APPLE;
            case SAPLING_GROWTH_MULTIPLIER -> Material.OAK_SAPLING;
            case FOOD_SATURATION_MULTIPLIER -> Material.COOKED_BEEF;
            case DIGGING_SPEED -> Material.GOLDEN_SHOVEL;
            case DIGGING_BLOCK_EXPERIENCE -> Material.EXPERIENCE_BOTTLE;
            case CROP_GROWTH_MULTIPLIER -> Material.BONE_MEAL;
            case FARMING_BONUS_DROP_CHANCE -> Material.WHEAT;
            case FARMING_HARVEST_EXPERIENCE -> Material.EXPERIENCE_BOTTLE;
            case ARMOR_MULTIPLIER -> Material.IRON_CHESTPLATE;
            case ARMOR_PENETRATION -> Material.ARMOR_STAND;
            case DODGE_CHANCE, MOVEMENT_PENALTY_REDUCTION, LIGHT_ARMOR_MOVEMENT_SPEED -> Material.FEATHER;
            case EXPERIENCE_MULTIPLIER, EXPERIENCE_ORB_MULTIPLIER -> Material.EXPERIENCE_BOTTLE;
            case REPUTATION_GAIN, VILLAGER_SKILL -> Material.EMERALD;
            case TRADE_DISCOUNT -> Material.GOLD_NUGGET;
            case TRADE_SELECTION_BONUS -> Material.LECTERN;
            case ITEM_QUALITY -> Material.SMITHING_TABLE;
            case ENCHANTMENT_POWER -> Material.ENCHANTED_BOOK;
            case EXPERIENCE_COST_REDUCTION -> Material.LAPIS_LAZULI;
            case RESOURCE_COST_REDUCTION -> Material.BUNDLE;
            case POTION_POWER -> Material.POTION;
            case BREWING_SPEED -> Material.BREWING_STAND;
            case THROWING_SPEED -> Material.SPLASH_POTION;
            case FURNACE_SPEED -> Material.BLAST_FURNACE;
            case FOOD_COOKING_SPEED -> Material.SMOKER;
            case TNT_POWER -> Material.TNT;
            case RARE_DROP_CHANCE -> Material.AMETHYST_SHARD;
            case ANIMAL_DAMAGE_MULTIPLIER, ANIMAL_BONUS_DROP_CHANCE -> Material.IRON_AXE;
            case ANIMAL_GROWTH_MULTIPLIER, BREEDING_EXPERIENCE_MULTIPLIER -> Material.WHEAT;
            case BEEKEEPING_HONEY_REFILL_CHANCE -> Material.HONEYCOMB;
            case LUCK -> Material.RABBIT_FOOT;
            case FISHING_SPEED -> Material.FISHING_ROD;
            case FISHING_TREASURE_CHANCE -> Material.HEART_OF_THE_SEA;
            case ACCURACY -> Material.TARGET;
            case AMMO_CONSUMPTION_REDUCTION -> Material.ARROW;
            case POWER_ATTACK_DAMAGE_MULTIPLIER -> Material.NETHERITE_AXE;
            case HUNGER_CONSUMPTION_REDUCTION -> Material.COOKED_BEEF;
            case DAMAGE_REFLECTION -> Material.SHIELD;
            case HEALTH_REGENERATION -> Material.GLISTERING_MELON_SLICE;
            case STATUS_IMMUNITY_REDUCTION -> Material.FERMENTED_SPIDER_EYE;
        };
    }

    private static Material mechanicMaterial(MechanicId mechanic) {
        return switch (mechanic) {
            case VEIN_MINING -> Material.DIAMOND_ORE;
            case DRILLING, TNT_MASTERY -> Material.TNT;
            case VEIN_CLUSTER_EXTRACTION -> Material.DEEPSLATE;
            case TREE_FELLER -> Material.OAK_LOG;
            case INSTANT_HARVEST, FIELD_HARVEST -> Material.GOLDEN_HOE;
            case BEEKEEPER -> Material.BEE_NEST;
            case GOLDEN_LEAVES -> Material.GOLDEN_APPLE;
            case INSTANT_LEAF_BREAK -> Material.SHEARS;
            case WEAPON_COATING -> Material.LINGERING_POTION;
            case CHARGED_SHOT, CUSTOM_ARROW_RECIPES -> Material.SPECTRAL_ARROW;
            case ADRENALINE -> Material.SUGAR;
            case RAGE -> Material.BLAZE_POWDER;
            case POTION_MERGING, ALCHEMY_RECIPES -> Material.BREWING_STAND;
            case HEXBLADE -> Material.ENCHANTED_BOOK;
            case PUNCH_HOLDING, UPPERCUT, DROPKICK -> Material.LEATHER_BOOTS;
            case GRAPPLE -> Material.LEAD;
            case MEDITATION -> Material.AMETHYST_SHARD;
            case VILLAGER_ORDERING, VILLAGER_UPGRADING, VILLAGER_TRAINING,
                    VILLAGER_GIFTS, BLACK_MARKET -> Material.EMERALD;
            case SMITHING_RECIPES, BULK_CRAFTING, TINKERING -> Material.SMITHING_TABLE;
            case RARE_LEAF_DROPS -> Material.OAK_LEAVES;
            case ARCHAEOLOGY_FINDS, SUSPICIOUS_BLOCK_RESTORATION -> Material.BRUSH;
            case GROUND_REPLICATION -> Material.GRASS_BLOCK;
            case FISHING_CHEST, FISHING_MASTER_CHEST -> Material.CHEST;
            case FISHING_WATER_ATTUNEMENT -> Material.BUBBLE_CORAL;
            case LIGHT_ARMOR_SET_BONUS, LIGHT_ARMOR_THREE_PIECE_SET_BONUS -> Material.CHAINMAIL_CHESTPLATE;
            case HEAVY_ARMOR_SET_BONUS -> Material.NETHERITE_CHESTPLATE;
            case HEAVY_ARMOR_THREE_PIECE_SET_BONUS -> Material.IRON_CHESTPLATE;
            case HEAVY_ARMOR_JUGGERNAUT -> Material.NETHERITE_HELMET;
            case LIGHT_ARMOR_CHAINMAIL_MOBILITY -> Material.CHAINMAIL_BOOTS;
            case LIGHT_ARMOR_DIAMOND_MOBILITY -> Material.DIAMOND_BOOTS;
            case HEAVY_ARMOR_IRON_MOBILITY -> Material.IRON_BOOTS;
            case HEAVY_ARMOR_NETHERITE_MOBILITY -> Material.NETHERITE_BOOTS;
            case LIGHT_WEAPON_IRON_MOBILITY, LIGHT_WEAPON_DIAMOND_MOBILITY,
                LIGHT_WEAPON_NETHERITE_MOBILITY -> Material.IRON_SWORD;
            case HEAVY_WEAPON_IRON_MOBILITY, HEAVY_WEAPON_DIAMOND_MOBILITY,
                HEAVY_WEAPON_NETHERITE_MOBILITY, HEAVY_POWER_SWEEP -> Material.IRON_AXE;
        };
    }
}
