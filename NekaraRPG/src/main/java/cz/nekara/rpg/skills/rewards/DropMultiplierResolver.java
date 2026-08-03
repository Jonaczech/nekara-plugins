package cz.nekara.rpg.skills.rewards;

import cz.nekara.rpg.skills.combat.ChanceRoller;
import cz.nekara.rpg.skills.stats.StatId;
import cz.nekara.rpg.skills.stats.StatSnapshot;
import java.util.Objects;

public final class DropMultiplierResolver {
    public int resolve(StatSnapshot stats, ChanceRoller chanceRoller) {
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(chanceRoller, "chanceRoller");
        if (chanceRoller.succeeds(stats.value(StatId.TRIPLE_DROP_CHANCE))) {
            return 3;
        }
        if (chanceRoller.succeeds(stats.value(StatId.DOUBLE_DROP_CHANCE))) {
            return 2;
        }
        return 1;
    }
}
