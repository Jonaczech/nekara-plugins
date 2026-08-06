package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.stats.StatId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.bukkit.Material;

public final class PerkIconResolver {
    private PerkIconResolver() {
    }

    public static Material resolve(PerkDefinition perk) {
        Objects.requireNonNull(perk, "perk");
        return perk.effects().stream()
            .min(Comparator.comparingInt(PerkIconResolver::priority))
            .map(PerkIconResolver::material)
            .orElse(Material.BOOK);
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
            case CRITICAL_CHANCE, CRITICAL_DAMAGE_MULTIPLIER -> Material.NETHERITE_SWORD;
            case BLEED_CHANCE, BLEED_DAMAGE_MULTIPLIER -> Material.REDSTONE;
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
            case DODGE_CHANCE, MOVEMENT_PENALTY_REDUCTION -> Material.FEATHER;
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
            case EQUIPMENT_FISHING, EQUIPMENT_SALVAGING -> Material.FISHING_ROD;
            case LIGHT_ARMOR_SET_BONUS -> Material.CHAINMAIL_CHESTPLATE;
            case HEAVY_ARMOR_SET_BONUS -> Material.NETHERITE_CHESTPLATE;
            case LIGHT_ARMOR_CHAINMAIL_MOBILITY -> Material.CHAINMAIL_BOOTS;
            case LIGHT_ARMOR_DIAMOND_MOBILITY -> Material.DIAMOND_BOOTS;
            case HEAVY_ARMOR_IRON_MOBILITY -> Material.IRON_BOOTS;
            case HEAVY_ARMOR_NETHERITE_MOBILITY -> Material.NETHERITE_BOOTS;
            case LIGHT_WEAPON_IRON_MOBILITY, LIGHT_WEAPON_DIAMOND_MOBILITY,
                LIGHT_WEAPON_NETHERITE_MOBILITY -> Material.IRON_SWORD;
            case HEAVY_WEAPON_IRON_MOBILITY, HEAVY_WEAPON_DIAMOND_MOBILITY,
                HEAVY_WEAPON_NETHERITE_MOBILITY -> Material.IRON_AXE;
        };
    }
}
