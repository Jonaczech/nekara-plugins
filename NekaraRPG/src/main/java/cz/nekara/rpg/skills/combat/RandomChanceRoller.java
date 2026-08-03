package cz.nekara.rpg.skills.combat;

import java.util.Objects;
import java.util.random.RandomGenerator;

public final class RandomChanceRoller implements ChanceRoller {
    private final RandomGenerator random;

    public RandomChanceRoller(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public boolean succeeds(double chance) {
        if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
            throw new IllegalArgumentException("Chance must be between 0 and 1");
        }
        return random.nextDouble() < chance;
    }
}
