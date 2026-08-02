package cz.nekara.rpg.configuration;

final class ConfigurationLayout {
    static final int CURRENT = 2;

    private ConfigurationLayout() {
    }

    static boolean requiresMigration(boolean hasExplicitLayout, int layout) {
        return !hasExplicitLayout || layout < CURRENT;
    }
}
