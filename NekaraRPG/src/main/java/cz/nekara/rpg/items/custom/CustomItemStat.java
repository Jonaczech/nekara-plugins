package cz.nekara.rpg.items.custom;

public enum CustomItemStat {
    ATTACK_DAMAGE("Poškození", -1024.0, 1024.0),
    ATTACK_SPEED("Rychlost útoku", -10.0, 100.0),
    ARMOR("Brnění", 0.0, 100.0),
    ARMOR_TOUGHNESS("Odolnost brnění", 0.0, 100.0),
    MAX_HEALTH_BONUS("Bonus zdraví", -20.0, 2048.0);

    private final String czechName;
    private final double minimum;
    private final double maximum;

    CustomItemStat(String czechName, double minimum, double maximum) {
        this.czechName = czechName;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public String czechName() {
        return czechName;
    }

    public double minimum() {
        return minimum;
    }

    public double maximum() {
        return maximum;
    }
}
