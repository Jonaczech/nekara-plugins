package cz.nekara.rpg.echovein;

import java.util.ArrayList;
import java.util.List;

/** Height-aware approximation of vanilla ore availability and relative rarity. */
public final class OreHeightDistribution {
    private OreHeightDistribution() {
    }

    public enum OreKind {
        COAL,
        COPPER,
        IRON,
        GOLD,
        REDSTONE,
        LAPIS,
        DIAMOND,
        NETHER_QUARTZ,
        NETHER_GOLD
    }

    public record WeightedOre(OreKind ore, int weight) {
        public WeightedOre {
            if (ore == null || weight <= 0) {
                throw new IllegalArgumentException("Ore and positive weight are required");
            }
        }
    }

    public static List<WeightedOre> overworld(int y, boolean badlands) {
        List<WeightedOre> ores = new ArrayList<>();
        add(ores, OreKind.COAL, coalWeight(y));
        add(ores, OreKind.COPPER, triangleWeight(y, 0, 48, 96, 5, 30));
        add(ores, OreKind.IRON, ironWeight(y));
        add(ores, OreKind.GOLD, goldWeight(y, badlands));
        add(ores, OreKind.REDSTONE, redstoneWeight(y));
        add(ores, OreKind.LAPIS, triangleWeight(y, -64, 0, 63, 1, 8));
        add(ores, OreKind.DIAMOND, diamondWeight(y));
        return List.copyOf(ores);
    }

    public static List<WeightedOre> nether(int y) {
        if (y < 10 || y > 117) {
            return List.of();
        }
        return List.of(
                new WeightedOre(OreKind.NETHER_QUARTZ, 85),
                new WeightedOre(OreKind.NETHER_GOLD, 15));
    }

    public static int totalWeight(List<WeightedOre> ores) {
        return ores.stream().mapToInt(WeightedOre::weight).sum();
    }

    public static OreKind select(List<WeightedOre> ores, int ticket) {
        int total = totalWeight(ores);
        if (ticket < 0 || ticket >= total) {
            return null;
        }
        int cursor = ticket;
        for (WeightedOre ore : ores) {
            if (cursor < ore.weight()) {
                return ore.ore();
            }
            cursor -= ore.weight();
        }
        return null;
    }

    private static int coalWeight(int y) {
        if (y <= 0) {
            return 0;
        }
        int distanceFromPeak = Math.min(96, Math.abs(y - 96));
        return 20 + (20 * (96 - distanceFromPeak) / 96);
    }

    private static int ironWeight(int y) {
        if (y < 72) {
            return triangleWeight(y, -64, 16, 71, 5, 30);
        }
        if (y > 80) {
            return Math.min(30, 5 + ((y - 80) / 8));
        }
        return 0;
    }

    private static int goldWeight(int y, boolean badlands) {
        int normal = triangleWeight(y, -64, -16, 31, 3, 12);
        if (normal > 0 && y < -48) {
            normal += 4;
        }
        if (badlands && y >= 32 && y <= 256) {
            return Math.max(normal, 8);
        }
        return normal;
    }

    private static int redstoneWeight(int y) {
        if (y < -64 || y >= 16) {
            return 0;
        }
        if (y >= -32) {
            return 8;
        }
        return 8 + ((-32 - y) * 17 / 32);
    }

    private static int diamondWeight(int y) {
        if (y < -64 || y >= 16) {
            return 0;
        }
        return 1 + ((15 - y) * 7 / 79);
    }

    private static int triangleWeight(
            int y,
            int minimumY,
            int peakY,
            int maximumY,
            int edgeWeight,
            int peakWeight
    ) {
        if (y < minimumY || y > maximumY) {
            return 0;
        }
        if (y == peakY) {
            return peakWeight;
        }
        int span = y < peakY ? peakY - minimumY : maximumY - peakY;
        int distance = Math.abs(y - peakY);
        return edgeWeight + ((peakWeight - edgeWeight) * (span - distance) / span);
    }

    private static void add(List<WeightedOre> ores, OreKind ore, int weight) {
        if (weight > 0) {
            ores.add(new WeightedOre(ore, weight));
        }
    }
}
