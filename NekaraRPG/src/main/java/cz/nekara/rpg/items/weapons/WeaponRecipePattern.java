package cz.nekara.rpg.items.weapons;

enum WeaponRecipePattern {
    DAGGER(" A ", " S "),
    GREATSWORD("AAA", " B ", " S "),
    HAMMER(" A ", "ASA", " S ");

    private final String[] rows;

    WeaponRecipePattern(String... rows) {
        this.rows = rows;
    }

    static WeaponRecipePattern forFamily(WeaponFamily family) {
        return switch (family) {
            case DAGGER -> DAGGER;
            case GREATSWORD -> GREATSWORD;
            case HAMMER -> HAMMER;
            default -> throw new IllegalArgumentException("No custom recipe pattern for " + family);
        };
    }

    String[] rows() {
        return rows.clone();
    }
}
