package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.skills.SkillId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Preserves small fractional rune bonuses until they form whole Skills XP. */
final class RuneExperienceAccumulator {
    private final Map<Key, Double> fractions = new HashMap<>();

    synchronized long claim(UUID playerId, SkillId skill, double amount) {
        if (amount <= 0.0) return 0L;
        Key key = new Key(playerId, skill);
        double total = fractions.getOrDefault(key, 0.0) + amount;
        long whole = (long) Math.floor(total + 1.0E-9);
        double remainder = total - whole;
        if (remainder <= 1.0E-9) fractions.remove(key);
        else fractions.put(key, remainder);
        return whole;
    }

    synchronized void clear() {
        fractions.clear();
    }

    private record Key(UUID playerId, SkillId skill) {
    }
}
