package cz.nekara.rpg.modules.skills;

import java.util.Locale;
import org.bukkit.Material;

final class ExperienceSourcePresentation {
    private ExperienceSourcePresentation() {
    }

    static String gathering(Material material) {
        return switch (material) {
            case STONE -> "Kámen";
            case DEEPSLATE -> "Hlubinná břidlice";
            case NETHERRACK -> "Netherový kámen";
            case END_STONE -> "Endový kámen";
            case COAL_ORE -> "Uhelná ruda";
            case DEEPSLATE_COAL_ORE -> "Hlubinná uhelná ruda";
            case COPPER_ORE -> "Měděná ruda";
            case DEEPSLATE_COPPER_ORE -> "Hlubinná měděná ruda";
            case IRON_ORE -> "Železná ruda";
            case DEEPSLATE_IRON_ORE -> "Hlubinná železná ruda";
            case NETHER_QUARTZ_ORE -> "Netherový křemen";
            case GOLD_ORE -> "Zlatá ruda";
            case DEEPSLATE_GOLD_ORE -> "Hlubinná zlatá ruda";
            case NETHER_GOLD_ORE -> "Netherová zlatá ruda";
            case REDSTONE_ORE -> "Redstone ruda";
            case DEEPSLATE_REDSTONE_ORE -> "Hlubinná redstone ruda";
            case LAPIS_ORE -> "Lapisová ruda";
            case DEEPSLATE_LAPIS_ORE -> "Hlubinná lapisová ruda";
            case DIAMOND_ORE -> "Diamantová ruda";
            case DEEPSLATE_DIAMOND_ORE -> "Hlubinná diamantová ruda";
            case EMERALD_ORE -> "Smaragdová ruda";
            case DEEPSLATE_EMERALD_ORE -> "Hlubinná smaragdová ruda";
            case ANCIENT_DEBRIS -> "Prastaré trosky";
            case OAK_LOG -> "Dubový kmen";
            case SPRUCE_LOG -> "Smrkový kmen";
            case BIRCH_LOG -> "Březový kmen";
            case JUNGLE_LOG -> "Kmen z džungle";
            case ACACIA_LOG -> "Akáciový kmen";
            case DARK_OAK_LOG -> "Kmen tmavého dubu";
            case MANGROVE_LOG -> "Mangrovový kmen";
            case CHERRY_LOG -> "Třešňový kmen";
            case PALE_OAK_LOG -> "Kmen bledého dubu";
            case CRIMSON_STEM -> "Karmínový kmen";
            case WARPED_STEM -> "Zprohýbaný kmen";
            case BAMBOO_BLOCK -> "Bambusový blok";
            case DIRT -> "Hlína";
            case GRASS_BLOCK -> "Travnatá hlína";
            case COARSE_DIRT -> "Hrubá hlína";
            case ROOTED_DIRT -> "Zakořeněná hlína";
            case PODZOL -> "Podzol";
            case MYCELIUM -> "Mycelium";
            case MUD -> "Bláto";
            case CLAY -> "Jíl";
            case SAND -> "Písek";
            case RED_SAND -> "Červený písek";
            case GRAVEL -> "Štěrk";
            case SOUL_SAND -> "Písek duší";
            case SOUL_SOIL -> "Půda duší";
            case SNOW_BLOCK -> "Sněhový blok";
            default -> readableMaterialName(material);
        };
    }

    static String activity(String sourceType) {
        return switch (sourceType) {
            case "combat_hit" -> "Bojový zásah";
            case "armor_hit" -> "Přijatý zásah";
            case "villager_trade" -> "Obchod s vesničanem";
            case "equipment_craft" -> "Výroba výbavy";
            case "smithing_table" -> "Kovářský stůl";
            case "enchant_item" -> "Očarování předmětu";
            case "brew_complete" -> "Uvařený lektvar";
            case "mature_harvest" -> "Zralá sklizeň";
            case "berry_harvest" -> "Sklizeň bobulí";
            case "wild_flower" -> "Přírodní květina";
            case "wild_mushroom" -> "Přírodní houba";
            case "grass_bundle" -> "Sběr trávy";
            case "vanilla_catch", "deferred_catch" -> "Úlovek";
            default -> "Dokončená činnost";
        };
    }

    private static String readableMaterialName(Material material) {
        String[] words = material.getKey().getKey().split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            name.append(word.substring(1));
        }
        return name.toString();
    }
}
