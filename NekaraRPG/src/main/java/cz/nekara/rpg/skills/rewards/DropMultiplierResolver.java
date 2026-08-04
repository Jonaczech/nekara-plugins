package cz.nekara.rpg.skills.rewards;

import cz.nekara.rpg.skills.combat.ChanceRoller;
import cz.nekara.rpg.skills.stats.StatId;
import cz.nekara.rpg.skills.stats.StatSnapshot;
import java.util.Objects;

public final class DropMultiplierResolver {
    public int resolve(StatSnapshot stats, ChanceRoller chanceRoller) {
        return resolve(stats, 0.0, chanceRoller);
    }

    public int resolve(StatSnapshot stats, double innateDoubleDropChance, ChanceRoller chanceRoller) {
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(chanceRoller, "chanceRoller");
        if (!Double.isFinite(innateDoubleDropChance)
            || innateDoubleDropChance < 0.0 || innateDoubleDropChance > 1.0) {
            throw new IllegalArgumentException("Innate double-drop chance must be between zero and one");
        }
        if (chanceRoller.succeeds(stats.value(StatId.TRIPLE_DROP_CHANCE))) {
            return 3;
        }
        if (chanceRoller.succeeds(Math.min(1.0,
                stats.value(StatId.DOUBLE_DROP_CHANCE) + innateDoubleDropChance))) {
            return 2;
        }
        return 1;
    }
}
