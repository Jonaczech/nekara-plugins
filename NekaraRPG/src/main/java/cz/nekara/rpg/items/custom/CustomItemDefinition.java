package cz.nekara.rpg.items.custom;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.bukkit.Material;

public record CustomItemDefinition(
        String id,
        Material material,
        String displayName,
        String modelKey,
        Integer customModelData,
        CustomItemStats stats
) {
    public static final int SCHEMA_VERSION = 1;
    private static final Pattern ID = Pattern.compile("[a-z0-9_-]{2,64}");
    private static final Pattern MODEL = Pattern.compile("[a-z0-9/._-]{2,128}");

    public CustomItemDefinition {
        id = normalizeId(id);
        material = Objects.requireNonNull(material, "material");
        displayName = Objects.requireNonNull(displayName, "displayName").trim();
        modelKey = Objects.requireNonNull(modelKey, "modelKey").trim().toLowerCase(Locale.ROOT);
        stats = Objects.requireNonNull(stats, "stats");
        if (!material.isItem()) {
            throw new IllegalArgumentException("Material must be an item");
        }
        int displayLength = displayName.codePointCount(0, displayName.length());
        if (displayLength < 1 || displayLength > 64 || displayName.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Display name must contain 1 to 64 printable characters");
        }
        if (!MODEL.matcher(modelKey).matches() || modelKey.contains("//") || modelKey.startsWith("/")
                || modelKey.endsWith("/")) {
            throw new IllegalArgumentException("Model key must be a lower-case resource path");
        }
        if (customModelData != null && customModelData < 1) {
            throw new IllegalArgumentException("Custom model data must be positive");
        }
    }

    public static String normalizeId(String value) {
        String normalized = Objects.requireNonNull(value, "id").trim().toLowerCase(Locale.ROOT);
        if (!ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException("ID must contain 2 to 64 lower-case letters, digits, dashes or underscores");
        }
        return normalized;
    }

    public CustomItemDefinition withId(String newId) {
        String normalized = normalizeId(newId);
        String nextModel = modelKey.equals("items/" + id) ? "items/" + normalized : modelKey;
        return new CustomItemDefinition(normalized, material, displayName, nextModel, customModelData, stats);
    }

    public CustomItemDefinition withMaterial(Material value) {
        return new CustomItemDefinition(id, value, displayName, modelKey, customModelData, stats);
    }

    public CustomItemDefinition withDisplayName(String value) {
        return new CustomItemDefinition(id, material, value, modelKey, customModelData, stats);
    }

    public CustomItemDefinition withModelKey(String value) {
        return new CustomItemDefinition(id, material, displayName, value, customModelData, stats);
    }

    public CustomItemDefinition withCustomModelData(Integer value) {
        return new CustomItemDefinition(id, material, displayName, modelKey, value, stats);
    }

    public CustomItemDefinition withStat(CustomItemStat stat, Double value) {
        return new CustomItemDefinition(id, material, displayName, modelKey, customModelData, stats.with(stat, value));
    }
}
