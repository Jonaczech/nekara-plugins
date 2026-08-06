package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.stats.ModifierOperation;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void everyCatalogPerkHasItsOwnThematicIcon() {
        DefaultPerkTree tree = DefaultPerkTree.create();
        List<Material> icons = SkillId.gameplaySkills().stream()
            .flatMap(skill -> tree.catalog().forSkill(skill).stream())
            .map(PerkIconResolver::resolve)
            .toList();

        assertEquals(tree.catalog().size(), icons.size());
        assertTrue(SkillId.gameplaySkills().stream()
            .flatMap(skill -> tree.catalog().forSkill(skill).stream())
            .allMatch(perk -> PerkIconResolver.hasCatalogIcon(perk.id())));
        assertEquals(icons.size(), icons.stream().collect(Collectors.toSet()).size());
        assertTrue(icons.stream().noneMatch(icon -> icon == Material.AIR));
    }

    private Material icon(PerkEffectDefinition... effects) {
        PerkDefinition perk = new PerkDefinition(
            new PerkId("test.icon"), SkillId.MARTIAL_ARTS, 1, 1, 0,
            Set.of(), List.of(effects), new PerkPosition(0, 0));
        return PerkIconResolver.resolve(perk);
    }
}
