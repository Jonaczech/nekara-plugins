package cz.nekara.rpg.skills.perks;

public record PerkPosition(int column, int row) {
    public PerkPosition {
        if (column < 0 || row < 0) {
            throw new IllegalArgumentException("Perk position coordinates must be non-negative");
        }
    }
}
