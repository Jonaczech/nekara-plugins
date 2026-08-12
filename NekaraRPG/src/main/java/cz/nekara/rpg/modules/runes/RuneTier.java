package cz.nekara.rpg.modules.runes;

enum RuneTier {
    I(1), II(2), III(3);

    private final int value;

    RuneTier(int value) {
        this.value = value;
    }

    int value() { return value; }

    static RuneTier fromValue(int value) {
        return value >= 3 ? III : value == 2 ? II : I;
    }
}
