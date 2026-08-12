package cz.nekara.rpg.items.custom;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class YamlCustomItemRepository implements CustomItemRepository {
    private final File file;
    private final YamlConfiguration yaml;

    public YamlCustomItemRepository(File file) throws IOException {
        this.file = file;
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create custom item storage directory: " + parent);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public List<CustomItemDefinition> findAll() {
        ConfigurationSection items = yaml.getConfigurationSection("items");
        if (items == null) {
            return List.of();
        }
        List<CustomItemDefinition> definitions = new ArrayList<>();
        for (String id : items.getKeys(false)) {
            find(id).ifPresent(definitions::add);
        }
        definitions.sort(Comparator.comparing(CustomItemDefinition::id));
        return List.copyOf(definitions);
    }

    @Override
    public Optional<CustomItemDefinition> find(String id) {
        String normalized;
        try {
            normalized = CustomItemDefinition.normalizeId(id);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        ConfigurationSection section = yaml.getConfigurationSection("items." + normalized);
        if (section == null) {
            return Optional.empty();
        }
        Material material = Material.matchMaterial(section.getString("material", ""));
        if (material == null) {
            return Optional.empty();
        }
        CustomItemStats stats = new CustomItemStats(
                optionalDouble(section, "stats.attack-damage"),
                optionalDouble(section, "stats.attack-speed"),
                optionalDouble(section, "stats.armor"),
                optionalDouble(section, "stats.armor-toughness"),
                optionalDouble(section, "stats.max-health-bonus")
        );
        Integer customModelData = section.contains("custom-model-data")
                ? section.getInt("custom-model-data") : null;
        try {
            return Optional.of(new CustomItemDefinition(
                    normalized,
                    material,
                    section.getString("display-name", normalized),
                    section.getString("model", "items/" + normalized),
                    customModelData,
                    stats
            ));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void create(CustomItemDefinition definition) throws IOException {
        if (find(definition.id()).isPresent()) {
            throw new IllegalArgumentException("Custom item ID already exists: " + definition.id());
        }
        write(definition);
    }

    @Override
    public void update(CustomItemDefinition definition) throws IOException {
        if (find(definition.id()).isEmpty()) {
            throw new IllegalArgumentException("Unknown custom item ID: " + definition.id());
        }
        write(definition);
    }

    private void write(CustomItemDefinition definition) throws IOException {
        String path = "items." + definition.id();
        yaml.set(path + ".schema", CustomItemDefinition.SCHEMA_VERSION);
        yaml.set(path + ".material", definition.material().getKey().getKey().toUpperCase(Locale.ROOT));
        yaml.set(path + ".display-name", definition.displayName());
        yaml.set(path + ".model", definition.modelKey());
        yaml.set(path + ".custom-model-data", definition.customModelData());
        setOptional(path + ".stats.attack-damage", definition.stats().attackDamage());
        setOptional(path + ".stats.attack-speed", definition.stats().attackSpeed());
        setOptional(path + ".stats.armor", definition.stats().armor());
        setOptional(path + ".stats.armor-toughness", definition.stats().armorToughness());
        setOptional(path + ".stats.max-health-bonus", definition.stats().maxHealthBonus());
        yaml.save(file);
    }

    private Double optionalDouble(ConfigurationSection section, String path) {
        return section.contains(path) ? section.getDouble(path) : null;
    }

    private void setOptional(String path, Double value) {
        yaml.set(path, value);
    }
}
