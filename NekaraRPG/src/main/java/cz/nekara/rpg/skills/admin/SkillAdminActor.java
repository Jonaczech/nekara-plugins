package cz.nekara.rpg.skills.admin;

import java.util.Objects;

public record SkillAdminActor(String key, String displayName) {
    public SkillAdminActor {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(displayName, "displayName");
        if (key.isBlank() || displayName.isBlank()) {
            throw new IllegalArgumentException("Administrative actor identity cannot be blank");
        }
    }
}
