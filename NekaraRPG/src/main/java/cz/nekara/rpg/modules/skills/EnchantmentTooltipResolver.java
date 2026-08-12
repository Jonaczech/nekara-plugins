package cz.nekara.rpg.modules.skills;

import java.util.List;
import java.util.Locale;

/** Player-facing numerical descriptions for the server's vanilla enchantments. */
final class EnchantmentTooltipResolver {
    private EnchantmentTooltipResolver() {
    }

    static List<String> explain(String key, int level) {
        int safeLevel = Math.max(1, level);
        return switch (key) {
            case "minecraft:protection" -> List.of(percent(4.0 * safeLevel)
                + " ochrany proti většině přímého poškození (společný limit 80 %).");
            case "minecraft:fire_protection" -> List.of(percent(8.0 * safeLevel)
                + " ochrany proti ohni a lávě (společný limit 80 %).");
            case "minecraft:feather_falling" -> List.of(percent(12.0 * safeLevel)
                + " ochrany proti pádu (společný limit 80 %).");
            case "minecraft:blast_protection" -> List.of(percent(8.0 * safeLevel)
                + " ochrany proti výbuchům (společný limit 80 %).");
            case "minecraft:projectile_protection" -> List.of(percent(8.0 * safeLevel)
                + " ochrany proti střelám (společný limit 80 %).");
            case "minecraft:respiration" -> List.of("+" + (15 * safeLevel)
                + " s dechu pod vodou; vyšší šance neudusit se.");
            case "minecraft:aqua_affinity" -> List.of("Ruší zpomalení těžby pod vodou.");
            case "minecraft:thorns" -> List.of(percent(15.0 * safeLevel)
                + " šance vrátit útočníkovi 1–4 poškození.");
            case "minecraft:depth_strider" -> List.of("Pohyb ve vodě: stupeň " + safeLevel + "/3.");
            case "minecraft:frost_walker" -> List.of("Mrazí vodu v dosahu " + (2 + safeLevel) + " bloků kolem hráče.");
            case "minecraft:soul_speed" -> List.of("Rychlejší pohyb po soul sand a soul soil: stupeň " + safeLevel + "/3.");
            case "minecraft:swift_sneak" -> List.of("Rychlost pohybu při plížení: +" + percent(15.0 * safeLevel) + ".");
            case "minecraft:sharpness" -> List.of("+" + decimal(0.5 * safeLevel + 0.5) + " poškození při zásahu živého cíle.");
            case "minecraft:smite" -> List.of("+" + decimal(2.5 * safeLevel) + " poškození proti nemrtvým.");
            case "minecraft:bane_of_arthropods" -> List.of("+" + decimal(2.5 * safeLevel) + " poškození proti členovcům.");
            case "minecraft:knockback" -> List.of("Vyšší odhození cíle: stupeň " + safeLevel + "/2.");
            case "minecraft:fire_aspect" -> List.of("Zapálí cíl na " + (4 * safeLevel) + " s.");
            case "minecraft:looting" -> List.of("Zvyšuje bonusový loot z nepřátel: stupeň " + safeLevel + "/3.");
            case "minecraft:sweeping_edge" -> List.of("Zvyšuje poškození plošného seku: stupeň " + safeLevel + "/3.");
            case "minecraft:efficiency" -> List.of("Zrychluje těžbu nástrojem: stupeň " + safeLevel + "/5.");
            case "minecraft:fortune" -> List.of("Zvyšuje množství vybraných dropů: stupeň " + safeLevel + "/3.");
            case "minecraft:silk_touch" -> List.of("Těžený blok obvykle upustí sám sebe.");
            case "minecraft:unbreaking" -> List.of("Snižuje spotřebu odolnosti: stupeň " + safeLevel + "/3.");
            case "minecraft:power" -> List.of("+" + percent(25.0 * safeLevel) + " poškození šípem.");
            case "minecraft:punch" -> List.of("Vyšší odhození šípem: stupeň " + safeLevel + "/2.");
            case "minecraft:flame" -> List.of("Šíp zapálí cíl na 5 s.");
            case "minecraft:infinity" -> List.of("Běžné šípy se při střelbě nespotřebovávají.");
            case "minecraft:luck_of_the_sea" -> List.of("Lepší rybářský loot: stupeň " + safeLevel + "/3.");
            case "minecraft:lure" -> List.of("Zkracuje čekání na záběr o " + (5 * safeLevel) + " s.");
            case "minecraft:loyalty" -> List.of("Trojzubec se vrací rychleji: stupeň " + safeLevel + "/3.");
            case "minecraft:impaling" -> List.of("+" + decimal(2.5 * safeLevel) + " poškození proti vodním tvorům.");
            case "minecraft:riptide" -> List.of("Vrhnutí hráče deštěm nebo vodou: stupeň " + safeLevel + "/3.");
            case "minecraft:channeling" -> List.of("Při bouřce může zásah trojzubcem přivolat blesk.");
            case "minecraft:multishot" -> List.of("Kuše vystřelí 3 projektily najednou.");
            case "minecraft:quick_charge" -> List.of("Rychlejší nabíjení kuše: stupeň " + safeLevel + "/3.");
            case "minecraft:piercing" -> List.of("Šíp z kuše projde až " + safeLevel + " dalšími cíli.");
            case "minecraft:density" -> List.of("+" + decimal(0.5 * safeLevel) + " poškození mace za každý blok pádu.");
            case "minecraft:breach" -> List.of("Ignoruje " + percent(15.0 * safeLevel) + " účinnosti zbroje cíle při zásahu mace.");
            case "minecraft:wind_burst" -> List.of("Úspěšný úder mace vyrazí hráče vzhůru: stupeň " + safeLevel + "/3.");
            case "minecraft:mending" -> List.of("Zkušenostní koule opravují náhodně vybranou poškozenou výbavu.");
            case "minecraft:binding_curse" -> List.of("Výbavu nelze běžně sundat, dokud hráč nezemře nebo není v kreativním režimu.");
            case "minecraft:vanishing_curse" -> List.of("Předmět po smrti hráče zmizí.");
            default -> List.of("Vanilla účinek, úroveň " + safeLevel + ".");
        };
    }

    private static String percent(double value) {
        return decimal(value) + " %";
    }

    private static String decimal(double value) {
        if (Math.rint(value) == value) return Integer.toString((int) value);
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
