package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.stats.ModifierOperation;
import cz.nekara.rpg.skills.stats.StatId;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Player-facing, quantified descriptions derived from the actual perk effects. */
public final class PerkEffectPresentation {
    private static final Locale CZECH_LOCALE = Locale.forLanguageTag("cs-CZ");

    private PerkEffectPresentation() {
    }

    public static List<String> describe(PerkDefinition perk, int currentRank) {
        Objects.requireNonNull(perk, "perk");
        int rank = Math.max(0, Math.min(currentRank, perk.maxRank()));
        int displayedRank = rank < perk.maxRank() ? rank + 1 : rank;
        List<String> descriptions = new ArrayList<>();
        for (PerkEffectDefinition effect : perk.effects()) {
            if (effect instanceof StatPerkEffect stat) {
                descriptions.add(describeStat(stat, displayedRank, perk.maxRank()));
            } else if (effect instanceof MechanicPerkEffect mechanic) {
                descriptions.add("Odemkne: " + mechanicDescription(mechanic.mechanicId()));
            }
        }
        return List.copyOf(descriptions);
    }

    private static String describeStat(StatPerkEffect effect, int displayedRank, int maximumRank) {
        double displayed = scaled(effect, displayedRank);
        String description = statDescription(effect.statId(), displayed);
        if (maximumRank > 1 && displayedRank < maximumRank) {
            return "Po hodnosti " + displayedRank + "/" + maximumRank + ": " + description
                + " (při " + maximumRank + "/" + maximumRank + ": "
                + statDescription(effect.statId(), scaled(effect, maximumRank)) + ")";
        }
        return (maximumRank > 1 ? "Při hodnosti " + displayedRank + "/" + maximumRank + ": " : "")
            + description;
    }

    private static double scaled(StatPerkEffect effect, int rank) {
        return switch (effect.operation()) {
            case ADD -> effect.amountPerRank() * rank;
            case MULTIPLY -> Math.pow(effect.amountPerRank(), rank);
        };
    }

    private static String statDescription(StatId stat, double value) {
        return switch (stat) {
            case DAMAGE_MULTIPLIER -> "poškození zbraní " + signedPercent(value);
            case CRITICAL_CHANCE -> "šance na kritický zásah " + signedPercentagePoints(value);
            case CRITICAL_DAMAGE_MULTIPLIER -> "násobitel kritického zásahu " + signedPercentagePoints(value);
            case BLEED_CHANCE -> "šance na krvácení " + signedPercentagePoints(value);
            case BLEED_DAMAGE_MULTIPLIER -> "násobitel poškození krvácením " + signedPercentagePoints(value);
            case STUN_CHANCE -> "šance na omráčení " + signedPercentagePoints(value);
            case DOUBLE_DROP_CHANCE -> "šance na dvojitý výtěžek " + signedPercentagePoints(value);
            case TRIPLE_DROP_CHANCE -> "šance na trojitý výtěžek " + signedPercentagePoints(value);
            case MINING_SPEED -> "rychlost dobývání kamene a rud " + signedPercent(value);
            case WOODCUTTING_SPEED -> "rychlost kácení " + signedPercent(value);
            case CROP_GROWTH_MULTIPLIER -> "rychlost růstu plodin " + signedPercent(value);
            case ARMOR_MULTIPLIER -> "účinnost zbroje " + signedPercent(value);
            case ARMOR_PENETRATION -> "průraznost zbroje " + signedPercentagePoints(value);
            case DODGE_CHANCE -> "šance zcela uhnout zásahu " + signedPercentagePoints(value);
            case EXPERIENCE_MULTIPLIER -> "získané XP této dovednosti " + signedPercent(value);
            case REPUTATION_GAIN -> "zisk reputace při obchodu " + signedPercent(value);
            case TRADE_DISCOUNT -> "vrácené smaragdy z ceny obchodu " + signedPercent(value);
            case TRADE_SELECTION_BONUS -> "bonus k rozsahu obchodních nabídek " + signedPercent(value);
            case VILLAGER_SKILL -> "účinnost vesničanských služeb " + signedPercent(value);
            case ITEM_QUALITY -> "šance posunout nově vyrobenou výbavu o jeden Tier výš "
                + signedPercentagePoints(value);
            case ENCHANTMENT_POWER -> "síla očarování " + signedPercent(value);
            case EXPERIENCE_COST_REDUCTION -> "úspora úrovní XP při očarování " + signedPercent(value);
            case RESOURCE_COST_REDUCTION -> "šance zachránit spotřebovanou přísadu " + signedPercentagePoints(value);
            case POTION_POWER -> "síla a délka účinku lektvarů " + signedPercent(value);
            case BREWING_SPEED -> "rychlost vaření lektvarů " + signedPercent(value);
            case THROWING_SPEED -> "rychlost vržených lektvarů " + signedPercent(value);
            case FURNACE_SPEED -> "rychlost tavení v peci " + signedPercent(value);
            case TNT_POWER -> "síla řízeného odstřelu " + signedPercent(value);
            case DIGGING_SPEED -> "rychlost kopání lopatou " + signedPercent(value);
            case RARE_DROP_CHANCE -> "šance na vzácný nález " + signedPercentagePoints(value);
            case ANIMAL_DAMAGE_MULTIPLIER -> "poškození proti zvířatům " + signedPercent(value);
            case BEEKEEPING_YIELD -> "výtěžek včelaření " + signedPercent(value);
            case FISHING_LUCK -> "rybářské štěstí " + signedPercent(value);
            case FISHING_SPEED -> "rychlost záběru " + signedPercent(value);
            case EXPERIENCE_ORB_MULTIPLIER -> "získané zkušenostní koule " + signedPercent(value);
            case ACCURACY -> "přesnost střel " + signedPercentagePoints(value);
            case AMMO_CONSUMPTION_REDUCTION -> "šance nevyčerpat šíp " + signedPercentagePoints(value);
            case POWER_ATTACK_DAMAGE_MULTIPLIER -> "poškození silového útoku " + signedPercent(value);
            case HUNGER_CONSUMPTION_REDUCTION -> "šance zabránit ztrátě hladu " + signedPercentagePoints(value);
            case MOVEMENT_PENALTY_REDUCTION -> "omezení pohybu od výstroje " + signedPercent(value);
            case DAMAGE_REFLECTION -> "odrážené přijaté poškození " + signedPercentagePoints(value);
            case HEALTH_REGENERATION -> "obnova zdraví " + signedPercent(value);
            case STATUS_IMMUNITY_REDUCTION -> "odolnost vůči stavovým účinkům " + signedPercentagePoints(value);
        };
    }

    private static String mechanicDescription(MechanicId mechanic) {
        return switch (mechanic) {
            case VEIN_MINING -> "při plížení vytěžení propojené přírodní rudné žíly";
            case DRILLING -> "řízený odstřel vlastním TNT; limity určuje serverová konfigurace";
            case TREE_FELLER -> "při plížení poražení propojeného přírodního stromu";
            case INSTANT_HARVEST -> "sklizeň a opětovné zasazení jedním dotykem";
            case FIELD_HARVEST -> "při plížení sklizeň zralých plodin v oblasti nejvýše 3×3";
            case PARRY -> "odražení útoku a krátké omráčení útočníka";
            case WEAPON_COATING -> "nanesení jednoho neokamžitého účinku lektvaru na 3 zásahy zbraně";
            case CHARGED_SHOT -> "nabité střely při plně nataženém luku nebo kuši";
            case ADRENALINE -> "Adrenalin při nízkém zdraví";
            case RAGE -> "Hněv při nízkém zdraví";
            case POTION_MERGING -> "spojení dvou pitných lektvarů a ametystového střepu do jedné lahve";
            case HEXBLADE -> "přeměna části zásahu na živlové poškození";
            case PUNCH_HOLDING -> "zesílený úder po vyčkání";
            case UPPERCUT -> "zvedák při plížení";
            case DROPKICK -> "odkopnutí při sprintu ve vzduchu";
            case GRAPPLE -> "sevření neozbrojenou rukou: 2 s Slowness III a Weakness I";
            case MEDITATION -> "meditace: Regeneration I na 5 s, jednou za 30 s";
            case VILLAGER_ORDERING -> "objednávání známých obchodů";
            case VILLAGER_UPGRADING -> "vylepšování vesničanských služeb";
            case VILLAGER_TRAINING -> "+1 zkušenost vesničana za obchod";
            case VILLAGER_GIFTS -> "5% šanci na dar po obchodu";
            case BLACK_MARKET -> "další 2% šanci na dar; 20% darů je ametystový střep";
            case SMITHING_RECIPES -> "Řemeslnickou soupravu: 4 železné nugety + papír + provázek";
            case BULK_CRAFTING -> "hromadnou výrobu a zpracování";
            case TINKERING -> "opravu 25 % odolnosti výbavy u smithing table za Řemeslnickou soupravu";
            case ALCHEMY_RECIPES -> "Tonikum vitality: water bottle + sweet berries + glow berries; Regeneration I na 45 s";
            case WOOD_RECIPES -> "pět prken z jednoho kmene nebo stonku místo čtyř";
            case RARE_LEAF_DROPS -> "vzácné nálezy z listí";
            case ARCHAEOLOGY_FINDS -> "rozšířené archeologické nálezy";
            case EQUIPMENT_FISHING -> "nalezenou výbavu v úlovcích";
            case EQUIPMENT_SALVAGING -> "rozebrání vylovené výbavy";
            case CUSTOM_ARROW_RECIPES -> "4 Šípy průzkumníka: 4 šípy + glow ink sac + ametystový střep; zásah označí cíl na 8 s";
            case LIGHT_ARMOR_SET_BONUS -> "bonus kompletní lehké výstroje";
            case HEAVY_ARMOR_SET_BONUS -> "bonus kompletní těžké výstroje";
        };
    }

    private static String signedPercent(double value) {
        return signed(value) + " %";
    }

    private static String signedPercentagePoints(double value) {
        return signed(value) + " p. b.";
    }

    private static String signed(double value) {
        return (value >= 0.0 ? "+" : "") + number(Math.abs(value) * 100.0);
    }

    private static String number(double value) {
        DecimalFormat format = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(CZECH_LOCALE));
        return format.format(value);
    }
}
