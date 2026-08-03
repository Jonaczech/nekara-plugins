package cz.nekara.rpg.skills.perks;

import java.util.Objects;
import java.util.regex.Pattern;

public record PerkId(String value) implements Comparable<PerkId> {
    private static final Pattern VALID_ID = Pattern.compile("[a-z][a-z0-9_]*\\.[a-z][a-z0-9_]*");

    public PerkId {
        Objects.requireNonNull(value, "value");
        if (!VALID_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("Perk ID must use the skill.perk_name format: " + value);
        }
    }

    @Override
    public int compareTo(PerkId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
