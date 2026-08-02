package cz.nekara.rpg.mount;

import java.util.Locale;
import java.util.Objects;

public final class MountOwnerId {
    private MountOwnerId() {
    }

    public static String fromPlayerName(String playerName) {
        String normalized = Objects.requireNonNull(playerName, "playerName").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be empty.");
        }
        return "name:" + normalized;
    }
}
