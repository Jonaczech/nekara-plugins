package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.profile.SkillProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkMechanicResolverTest {
    private final PerkMechanicResolver resolver = new PerkMechanicResolver(
        DefaultPerkTree.create().catalog());

    @Test
    void onlyPurchasedMechanicsAreExposed() {
        SkillProfile profile = new SkillProfile(
            "player",
            Map.of(),
            Map.of(new PerkId("mining.vein"), 1),
            3,
            0
        );

        assertTrue(resolver.has(profile, SkillId.MINING, MechanicId.VEIN_MINING));
        assertFalse(resolver.has(profile, SkillId.MINING, MechanicId.DRILLING));
        assertFalse(resolver.has(profile, SkillId.WOODCUTTING, MechanicId.VEIN_MINING));
    }
}
