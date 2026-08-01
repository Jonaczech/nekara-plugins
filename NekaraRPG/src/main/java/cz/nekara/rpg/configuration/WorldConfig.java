package cz.nekara.rpg.configuration;

import java.util.Set;

public record WorldConfig(WorldMode mode, Set<String> worlds) {
    public boolean isEnabled(String worldName) {
        return switch (mode) {
            case ALL -> true;
            case WHITELIST -> worlds.contains(worldName);
            case BLACKLIST -> !worlds.contains(worldName);
        };
    }
}
