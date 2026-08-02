package cz.nekara.rpg.echovein;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.nekara.rpg.echovein.OreHeightDistribution.OreKind;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreHeightDistributionTest {
    @Test
    void highStoneCannotRevealDeepOres() {
        Set<OreKind> ores = kinds(OreHeightDistribution.overworld(70, false));
        assertEquals(Set.of(OreKind.COAL, OreKind.COPPER, OreKind.IRON), ores);
        assertFalse(ores.contains(OreKind.DIAMOND));
        assertFalse(ores.contains(OreKind.REDSTONE));
    }

    @Test
    void diamondAndRedstoneStopAtSixteenAndGrowDeeper() {
        List<OreHeightDistribution.WeightedOre> atFifteen =
                OreHeightDistribution.overworld(15, false);
        List<OreHeightDistribution.WeightedOre> atSixteen =
                OreHeightDistribution.overworld(16, false);
        assertTrue(kinds(atFifteen).contains(OreKind.DIAMOND));
        assertTrue(kinds(atFifteen).contains(OreKind.REDSTONE));
        assertFalse(kinds(atSixteen).contains(OreKind.DIAMOND));
        assertFalse(kinds(atSixteen).contains(OreKind.REDSTONE));
        assertTrue(weight(OreHeightDistribution.overworld(-64, false), OreKind.DIAMOND)
                > weight(atFifteen, OreKind.DIAMOND));
        assertTrue(weight(OreHeightDistribution.overworld(-64, false), OreKind.REDSTONE)
                > weight(atFifteen, OreKind.REDSTONE));
    }

    @Test
    void copperAndIronRespectTheirVanillaBands() {
        assertTrue(kinds(OreHeightDistribution.overworld(48, false)).contains(OreKind.COPPER));
        assertFalse(kinds(OreHeightDistribution.overworld(97, false)).contains(OreKind.COPPER));
        assertTrue(kinds(OreHeightDistribution.overworld(71, false)).contains(OreKind.IRON));
        assertFalse(kinds(OreHeightDistribution.overworld(72, false)).contains(OreKind.IRON));
        assertFalse(kinds(OreHeightDistribution.overworld(80, false)).contains(OreKind.IRON));
        assertTrue(kinds(OreHeightDistribution.overworld(81, false)).contains(OreKind.IRON));
    }

    @Test
    void badlandsAllowHighGold() {
        assertFalse(kinds(OreHeightDistribution.overworld(70, false)).contains(OreKind.GOLD));
        assertTrue(kinds(OreHeightDistribution.overworld(70, true)).contains(OreKind.GOLD));
    }

    @Test
    void netherOresStayInsideTheirGenerationBand() {
        assertTrue(OreHeightDistribution.nether(9).isEmpty());
        assertEquals(Set.of(OreKind.NETHER_QUARTZ, OreKind.NETHER_GOLD),
                kinds(OreHeightDistribution.nether(10)));
        assertEquals(Set.of(OreKind.NETHER_QUARTZ, OreKind.NETHER_GOLD),
                kinds(OreHeightDistribution.nether(117)));
        assertTrue(OreHeightDistribution.nether(118).isEmpty());
    }

    @Test
    void weightedSelectionUsesExactBoundaries() {
        List<OreHeightDistribution.WeightedOre> ores = List.of(
                new OreHeightDistribution.WeightedOre(OreKind.COAL, 2),
                new OreHeightDistribution.WeightedOre(OreKind.IRON, 1));
        assertEquals(3, OreHeightDistribution.totalWeight(ores));
        assertEquals(OreKind.COAL, OreHeightDistribution.select(ores, 0));
        assertEquals(OreKind.COAL, OreHeightDistribution.select(ores, 1));
        assertEquals(OreKind.IRON, OreHeightDistribution.select(ores, 2));
        assertNull(OreHeightDistribution.select(ores, 3));
    }

    private static Set<OreKind> kinds(List<OreHeightDistribution.WeightedOre> ores) {
        return ores.stream().map(OreHeightDistribution.WeightedOre::ore).collect(Collectors.toSet());
    }

    private static int weight(List<OreHeightDistribution.WeightedOre> ores, OreKind kind) {
        return ores.stream().filter(ore -> ore.ore() == kind).findFirst()
                .map(OreHeightDistribution.WeightedOre::weight).orElse(0);
    }
}
