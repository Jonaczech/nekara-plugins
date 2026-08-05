package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cz.nekara.rpg.skills.SkillId;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class SkillIconResolverTest {
    @Test
    void activeSkillsUseTheAssignedPotterySherds() {
        Map<SkillId, Material> expected = Map.ofEntries(
            Map.entry(SkillId.MINING, Material.MINER_POTTERY_SHERD),
            Map.entry(SkillId.ARCHERY, Material.ARCHER_POTTERY_SHERD),
            Map.entry(SkillId.DIGGING, Material.EXPLORER_POTTERY_SHERD),
            Map.entry(SkillId.FISHING, Material.ANGLER_POTTERY_SHERD),
            Map.entry(SkillId.LIGHT_WEAPONS, Material.BLADE_POTTERY_SHERD),
            Map.entry(SkillId.HEAVY_WEAPONS, Material.SCRAPE_POTTERY_SHERD),
            Map.entry(SkillId.SMITHING, Material.PRIZE_POTTERY_SHERD),
            Map.entry(SkillId.WOODCUTTING, Material.SHELTER_POTTERY_SHERD),
            Map.entry(SkillId.ALCHEMY, Material.BREWER_POTTERY_SHERD),
            Map.entry(SkillId.FARMING, Material.HOWL_POTTERY_SHERD),
            Map.entry(SkillId.ENCHANTING, Material.FLOW_POTTERY_SHERD),
            Map.entry(SkillId.LIGHT_ARMOR, Material.ARMS_UP_POTTERY_SHERD),
            Map.entry(SkillId.HEAVY_ARMOR, Material.MOURNER_POTTERY_SHERD),
            Map.entry(SkillId.POWER, Material.PLAYER_HEAD)
        );

        expected.forEach((skill, icon) -> assertEquals(icon, SkillIconResolver.resolve(skill), skill.name()));
    }
}
