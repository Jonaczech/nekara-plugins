package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.stats.ModifierOperation;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PerkIconResolverTest {
    @Test
    void distinctiveCombatEffectsWinOverGenericDamage() {
        assertEquals(Material.REDSTONE, icon(
            new StatPerkEffect(StatId.DAMAGE_MULTIPLIER, ModifierOperation.ADD, 0.1),
            new StatPerkEffect(StatId.BLEED_CHANCE, ModifierOperation.ADD, 0.1)));
        assertEquals(Material.MACE, icon(
            new StatPerkEffect(StatId.DAMAGE_MULTIPLIER, ModifierOperation.ADD, 0.1),
            new StatPerkEffect(StatId.STUN_CHANCE, ModifierOperation.ADD, 0.1)));
        assertEquals(Material.NETHERITE_SWORD, icon(
            new StatPerkEffect(StatId.CRITICAL_CHANCE, ModifierOperation.ADD, 0.1)));
    }

    @Test
    void everyStatHasAVisibleIcon() {
        for (StatId stat : StatId.values()) {
            assertNotEquals(Material.AIR, icon(
                new StatPerkEffect(stat, ModifierOperation.ADD, 0.01)), stat.name());
        }
    }

    private Material icon(PerkEffectDefinition... effects) {
        PerkDefinition perk = new PerkDefinition(
            new PerkId("test.icon"), SkillId.MARTIAL_ARTS, 1, 1, 0,
            Set.of(), List.of(effects), new PerkPosition(0, 0));
        return PerkIconResolver.resolve(perk);
    }
}
