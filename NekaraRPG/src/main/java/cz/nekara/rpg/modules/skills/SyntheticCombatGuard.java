package cz.nekara.rpg.modules.skills;

final class SyntheticCombatGuard {
    private enum DamageOrigin { NONE, GENERIC, BLEED }

    private static final ThreadLocal<DamageOrigin> ORIGIN = ThreadLocal.withInitial(() -> DamageOrigin.NONE);

    private SyntheticCombatGuard() {
    }

    static boolean isActive() {
        return ORIGIN.get() != DamageOrigin.NONE;
    }

    static boolean isBleed() { return ORIGIN.get() == DamageOrigin.BLEED; }

    static void run(Runnable action) {
        run(DamageOrigin.GENERIC, action);
    }

    static void runBleed(Runnable action) {
        run(DamageOrigin.BLEED, action);
    }

    private static void run(DamageOrigin origin, Runnable action) {
        DamageOrigin previous = ORIGIN.get();
        ORIGIN.set(origin);
        try {
            action.run();
        } finally {
            ORIGIN.set(previous);
        }
    }
}
