package cz.nekara.rpg.skills.perks;

import java.util.Objects;

public record PerkPresentation(String name, String description) {
    public PerkPresentation {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        if (name.isBlank() || description.isBlank()) {
            throw new IllegalArgumentException("Perk presentation cannot be blank");
        }
    }
}
