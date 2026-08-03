package cz.nekara.rpg.modules.skills;

final class SyntheticCombatGuard {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private SyntheticCombatGuard() {
    }

    static boolean isActive() {
        return ACTIVE.get();
    }

    static void run(Runnable action) {
        boolean previous = ACTIVE.get();
        ACTIVE.set(true);
        try {
            action.run();
        } finally {
            ACTIVE.set(previous);
        }
    }
}
