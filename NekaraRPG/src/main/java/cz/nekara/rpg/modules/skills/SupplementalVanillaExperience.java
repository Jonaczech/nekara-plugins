package cz.nekara.rpg.modules.skills;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Converts fractional perk XP into whole points without losing small rewards to rounding.
 */
final class SupplementalVanillaExperience {
    private final Map<UUID, Account> accounts = new HashMap<>();

    int claim(UUID playerId, double amount) {
        if (amount <= 0.0) {
            return 0;
        }
        Account account = accounts.computeIfAbsent(playerId, ignored -> new Account());
        account.fractionalExperience += amount;
        int requested = (int) Math.floor(account.fractionalExperience);
        account.fractionalExperience -= requested;
        return requested;
    }

    void forget(UUID playerId) {
        accounts.remove(playerId);
    }

    void clear() {
        accounts.clear();
    }

    private static final class Account {
        private double fractionalExperience;

        private Account() {
        }
    }
}
