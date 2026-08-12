package cz.nekara.rpg.modules.runes;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunePolicyTest {
    @Test
    void tiersRequireTheRunePerkAndFollowRunotepectviLevels() {
        assertThrows(IllegalArgumentException.class, () -> RunePolicy.tierFor(1, 0, 0, 0));
        assertEquals(RuneTier.I, RunePolicy.tierFor(1, 1, 0, 0));
        assertEquals(RuneTier.I, RunePolicy.tierFor(30, 1, 2, 0));
        assertEquals(RuneTier.II, RunePolicy.tierFor(30, 1, 3, 0));
        assertEquals(RuneTier.II, RunePolicy.tierFor(70, 1, 3, 2));
        assertEquals(RuneTier.III, RunePolicy.tierFor(70, 1, 3, 3));
    }

    @Test
    void eachEnchantingRowHasItsOwnServerAuthoritativeRequirementsAndCosts() {
        assertTrue(RunePolicy.canSelectTier(RuneTier.I, 1, 1, 0, 0));
        assertFalse(RunePolicy.canSelectTier(RuneTier.II, 30, 1, 2, 0));
        assertTrue(RunePolicy.canSelectTier(RuneTier.II, 30, 1, 3, 0));
        assertFalse(RunePolicy.canSelectTier(RuneTier.III, 70, 1, 3, 2));
        assertTrue(RunePolicy.canSelectTier(RuneTier.III, 70, 1, 3, 3));
        assertEquals(6, RunePolicy.baseExperienceCost(RuneTier.I));
        assertEquals(5, RunePolicy.experienceCost(RuneTier.I, 0.20));
        assertEquals(9, RunePolicy.baseExperienceCost(RuneTier.II));
        assertEquals(8, RunePolicy.experienceCost(RuneTier.II, 0.20));
        assertEquals(12, RunePolicy.baseExperienceCost(RuneTier.III));
        assertEquals(10, RunePolicy.experienceCost(RuneTier.III, 0.20));
        assertEquals(1, RunePolicy.dyeCost(RuneTier.I));
        assertEquals(2, RunePolicy.dyeCost(RuneTier.II));
        assertEquals(3, RunePolicy.dyeCost(RuneTier.III));
    }

    @Test
    void pigmentPreservationAndMemoryRefundAreBoundedAndDeterministic() {
        assertTrue(RunePolicy.preservesDye(0.15, 0.149));
        assertFalse(RunePolicy.preservesDye(0.15, 0.15));
        assertFalse(RunePolicy.preservesDye(-1.0, 0.0));
        assertEquals(2, RunePolicy.memoryExperienceRefund(RuneTier.I));
        assertEquals(2, RunePolicy.memoryExperienceRefund(RuneTier.II));
        assertEquals(3, RunePolicy.memoryExperienceRefund(RuneTier.III));
    }

    @Test
    void newGamePlusStrengthensRuneMemoryWithoutCreatingASeparateProc() {
        assertEquals(0.0, RunePolicy.engravingReturnChance(false, false));
        assertEquals(0.10, RunePolicy.engravingReturnChance(true, false));
        assertEquals(0.20, RunePolicy.engravingReturnChance(true, true));
    }

    @Test
    void runeTargetsOnlyAcceptTheirCompatibleEquipment() {
        assertTrue(RunePolicy.supports(RuneTarget.WEAPON, Material.IRON_SWORD));
        assertTrue(RunePolicy.supports(RuneTarget.BOW, Material.BOW));
        assertTrue(RunePolicy.supports(RuneTarget.BOOTS, Material.DIAMOND_BOOTS));
        assertTrue(RunePolicy.supports(RuneTarget.TOOL, Material.IRON_PICKAXE));
        assertFalse(RunePolicy.supports(RuneTarget.TOOL, Material.IRON_SWORD));
        assertTrue(RunePolicy.supports(RuneTarget.EQUIPMENT, Material.DIAMOND_CHESTPLATE));
        assertFalse(RunePolicy.supports(RuneTarget.EQUIPMENT, Material.DIRT));
    }

    @Test
    void onlyWhiteDyeCreatesTheCurrentInsightRune() {
        assertEquals(RuneEffect.INSIGHT, RuneEffect.byDye(Material.WHITE_DYE).orElseThrow());
        assertTrue(RuneEffect.byDye(Material.RED_DYE).isEmpty());
    }
}
