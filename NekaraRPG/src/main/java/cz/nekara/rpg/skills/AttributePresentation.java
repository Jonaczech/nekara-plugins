package cz.nekara.rpg.skills;

import java.util.Locale;

/**
 * Czech formatting shared by the live character overview.
 */
public final class AttributePresentation {
    private AttributePresentation() {
    }

    public static String percentage(double ratio) {
        return decimal(ratio * 100.0) + " %";
    }

    public static String bonusPercentage(double multiplier) {
        return signedPercentage(multiplier - 1.0);
    }

    public static String signedPercentage(double ratio) {
        double percentage = ratio * 100.0;
        return (percentage > 0.0 ? "+" : "") + decimal(percentage) + " %";
    }

    public static String decimal(double value) {
        String formatted = String.format(Locale.ROOT, "%.1f", value);
        return (formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted)
            .replace('.', ',');
    }
}
