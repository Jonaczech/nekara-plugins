package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.skills.SkillId;
import java.util.Objects;
import org.bukkit.Material;

final class SkillIconResolver {
    private SkillIconResolver() {
    }

    static Material resolve(SkillId skill) {
        Objects.requireNonNull(skill, "skill");
        return switch (skill) {
            case MARTIAL_ARTS -> Material.IRON_SWORD;
            case TRADING -> Material.EMERALD;
            case SMITHING -> Material.PRIZE_POTTERY_SHERD;
            case ENCHANTING -> Material.FLOW_POTTERY_SHERD;
            case ALCHEMY -> Material.BREWER_POTTERY_SHERD;
            case MINING -> Material.MINER_POTTERY_SHERD;
            case WOODCUTTING -> Material.SHELTER_POTTERY_SHERD;
            case DIGGING -> Material.EXPLORER_POTTERY_SHERD;
            case FARMING -> Material.HOWL_POTTERY_SHERD;
            case FISHING -> Material.ANGLER_POTTERY_SHERD;
            case LIGHT_WEAPONS -> Material.BLADE_POTTERY_SHERD;
            case HEAVY_WEAPONS -> Material.SCRAPE_POTTERY_SHERD;
            case ARCHERY -> Material.ARCHER_POTTERY_SHERD;
            case LIGHT_ARMOR -> Material.ARMS_UP_POTTERY_SHERD;
            case HEAVY_ARMOR -> Material.MOURNER_POTTERY_SHERD;
            case POWER -> Material.PLAYER_HEAD;
        };
    }
}
