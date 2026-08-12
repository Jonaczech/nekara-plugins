package cz.nekara.rpg.modules.runes;

import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

final class RuneItemFactory {
    private static final String BLANK = "blank";
    private static final String MAGICAL = "magical";
    private static final String UNSTABLE = "unstable";
    private static final String AWAKENED = "awakened";
    private static final NamespacedKey PLACEHOLDER_MODEL = new NamespacedKey("nekararpg", "runes/placeholder");
    private final NamespacedKey typeKey;
    private final NamespacedKey stageKey;
    private final NamespacedKey effectKey;
    private final NamespacedKey tierKey;

    RuneItemFactory(JavaPlugin plugin) {
        typeKey = new NamespacedKey(plugin, "rune_type");
        stageKey = new NamespacedKey(plugin, "rune_stage");
        effectKey = new NamespacedKey(plugin, "rune_effect");
        tierKey = new NamespacedKey(plugin, "rune_tier");
    }

    ItemStack blank() { return rune(BLANK, null, null); }
    ItemStack magical() { return rune(MAGICAL, null, null); }
    ItemStack unstable(RuneEffect effect, RuneTier tier) { return rune(UNSTABLE, effect, tier); }
    ItemStack awakened(RuneEffect effect, RuneTier tier) { return rune(AWAKENED, effect, tier); }

    boolean isBlank(ItemStack item) { return stage(item).filter(BLANK::equals).isPresent(); }
    boolean isMagical(ItemStack item) { return stage(item).filter(MAGICAL::equals).isPresent(); }
    boolean isUnstable(ItemStack item) { return stage(item).filter(UNSTABLE::equals).isPresent(); }
    boolean isAwakened(ItemStack item) { return stage(item).filter(AWAKENED::equals).isPresent(); }

    Optional<RuneEffect> effect(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        String value = item.getItemMeta().getPersistentDataContainer().get(effectKey, PersistentDataType.STRING);
        return value == null ? Optional.empty() : RuneEffect.byId(value);
    }

    Optional<RuneTier> tier(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        Integer value = item.getItemMeta().getPersistentDataContainer().get(tierKey, PersistentDataType.INTEGER);
        return value == null ? Optional.empty() : Optional.of(RuneTier.fromValue(value));
    }

    ItemStack embed(ItemStack equipment, RuneEffect effect, RuneTier tier) {
        return RuneSocketData.embed(equipment, effect, tier);
    }

    private ItemStack rune(String stage, RuneEffect effect, RuneTier tier) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(PLACEHOLDER_MODEL);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "rune");
        meta.getPersistentDataContainer().set(stageKey, PersistentDataType.STRING, stage);
        if (effect != null && tier != null) {
            meta.getPersistentDataContainer().set(effectKey, PersistentDataType.STRING, effect.id());
            meta.getPersistentDataContainer().set(tierKey, PersistentDataType.INTEGER, tier.value());
        }
        switch (stage) {
            case BLANK -> {
                meta.displayName(Component.text("Prázdná runa", NamedTextColor.GRAY));
                meta.lore(List.of(Component.text("Kamenné jádro čeká na magické probuzení.", NamedTextColor.DARK_GRAY)));
            }
            case MAGICAL -> {
                meta.displayName(Component.text("Magická runa", NamedTextColor.LIGHT_PURPLE));
                meta.setEnchantmentGlintOverride(true);
                meta.lore(List.of(Component.text("Neprobuzená energie čeká na zápis.", NamedTextColor.DARK_GRAY)));
            }
            case UNSTABLE -> {
                meta.displayName(Component.text("Nestabilní " + effect.displayName(), NamedTextColor.LIGHT_PURPLE));
                meta.setEnchantmentGlintOverride(true);
                meta.lore(List.of(Component.text("Tier " + tier.name() + " · " + effect.description(tier), NamedTextColor.GRAY),
                    Component.text("Klikni s ní na prázdný lectern.", NamedTextColor.YELLOW)));
            }
            case AWAKENED -> {
                meta.displayName(Component.text(effect.displayName() + " " + tier.name(), NamedTextColor.AQUA));
                meta.setEnchantmentGlintOverride(true);
                meta.lore(List.of(Component.text(effect.description(tier), NamedTextColor.GRAY),
                    Component.text("Vkovává se do volného socketu na kovadlině.", NamedTextColor.DARK_GRAY)));
            }
            default -> throw new IllegalArgumentException("Unknown rune stage");
        }
        item.setItemMeta(meta);
        return item;
    }

    private Optional<String> stage(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        var data = item.getItemMeta().getPersistentDataContainer();
        if (!"rune".equals(data.get(typeKey, PersistentDataType.STRING))) return Optional.empty();
        return Optional.ofNullable(data.get(stageKey, PersistentDataType.STRING));
    }
}
