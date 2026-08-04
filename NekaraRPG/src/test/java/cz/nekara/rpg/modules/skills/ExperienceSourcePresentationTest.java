package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ExperienceSourcePresentationTest {
    @Test
    void presentsKnownGatheringSourcesInCzech() {
        assertEquals("Kámen", ExperienceSourcePresentation.gathering(Material.STONE));
        assertEquals("Diamantová ruda", ExperienceSourcePresentation.gathering(Material.DIAMOND_ORE));
        assertEquals("Dubový kmen", ExperienceSourcePresentation.gathering(Material.OAK_LOG));
        assertEquals("Hlína", ExperienceSourcePresentation.gathering(Material.DIRT));
    }

    @Test
    void presentsActivitySourcesWithoutInternalEventNames() {
        assertEquals("Obchod s vesničanem", ExperienceSourcePresentation.activity("villager_trade"));
        assertEquals("Přírodní květina", ExperienceSourcePresentation.activity("wild_flower"));
        assertEquals("Sklizeň bobulí", ExperienceSourcePresentation.activity("berry_harvest"));
        assertEquals("Úlovek", ExperienceSourcePresentation.activity("deferred_catch"));
        assertEquals("Dokončená činnost", ExperienceSourcePresentation.activity("unknown"));
    }
}
