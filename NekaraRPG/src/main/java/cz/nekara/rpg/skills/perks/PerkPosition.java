package cz.nekara.rpg.skills.perks;

public record PerkPosition(int column, int row) {
    public PerkPosition {
        if (column < 0 || column > 8 || row < 0 || row > 5) {
            throw new IllegalArgumentException("Perk position must fit a 9x6 inventory viewport");
        }
    }
}
