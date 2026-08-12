package cz.nekara.rpg.modules.runes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Persistent, repeatable rune sockets stored directly on quality equipment. */
public final class RuneSocketData {
    private static final NamespacedKey QUALITY_KEY = new NamespacedKey("nekararpg", "smithing_tier");
    private static final NamespacedKey SOCKETS_KEY = new NamespacedKey("nekararpg", "embedded_rune_sockets");
    private static final NamespacedKey LEGACY_EFFECT_KEY = new NamespacedKey("nekararpg", "embedded_rune_effect");
    private static final NamespacedKey LEGACY_TIER_KEY = new NamespacedKey("nekararpg", "embedded_rune_tier");
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private RuneSocketData() {
    }

    static int capacity(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        Integer quality = item.getPersistentDataContainer().get(QUALITY_KEY, PersistentDataType.INTEGER);
        return RuneSocketPolicy.capacityForQuality(quality == null ? 0 : quality);
    }

    static boolean canEmbed(ItemStack item, RuneEffect effect, RuneTier tier) {
        return item != null && effect != null && tier != null
            && RunePolicy.supports(effect.target(), item.getType())
            && entries(item).size() < capacity(item);
    }

    static ItemStack embed(ItemStack equipment, RuneEffect effect, RuneTier tier) {
        if (!canEmbed(equipment, effect, tier)) return equipment.clone();
        ItemStack result = equipment.clone();
        List<Entry> currentEntries = new ArrayList<>(entries(result));
        currentEntries.add(new Entry(effect, tier));
        ItemMeta meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(SOCKETS_KEY, PersistentDataType.STRING, serialize(currentEntries));
        meta.getPersistentDataContainer().remove(LEGACY_EFFECT_KEY);
        meta.getPersistentDataContainer().remove(LEGACY_TIER_KEY);
        result.setItemMeta(meta);
        refreshLore(result);
        return result;
    }

    static Optional<RuneTier> firstTier(ItemStack item, RuneEffect expected) {
        return entries(item).stream().filter(entry -> entry.effect == expected).map(Entry::tier).findFirst();
    }

    public static double equippedExperienceMultiplier(Player player) {
        double bonus = experienceBonus(player.getInventory().getItemInMainHand())
            + experienceBonus(player.getInventory().getItemInOffHand());
        for (ItemStack armor : player.getInventory().getArmorContents()) bonus += experienceBonus(armor);
        return 1.0 + bonus;
    }

    public static void refreshLore(ItemStack item) {
        int capacity = capacity(item);
        if (capacity < 1) return;
        List<Entry> currentEntries = entries(item);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        removeManagedLore(lore);
        lore.add(Component.empty());
        NamedTextColor qualityColor = qualityColor(item);
        Component sockets = Component.text("Sockety: ", qualityColor);
        for (int slot = 0; slot < capacity; slot++) {
            sockets = sockets.append(Component.text(slot < currentEntries.size() ? "\u25C6" : "\u25C7",
                slot < currentEntries.size() ? qualityColor : NamedTextColor.DARK_GRAY));
            if (slot + 1 < capacity) sockets = sockets.append(Component.space());
        }
        lore.add(sockets);
        for (Entry entry : currentEntries) {
            lore.add(Component.text("  \u25C6 " + entry.effect.displayName() + " " + entry.tier.name(), qualityColor));
            lore.add(Component.text("    " + entry.effect.description(entry.tier), NamedTextColor.DARK_GRAY));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private static double experienceBonus(ItemStack item) {
        return entries(item).stream().filter(entry -> entry.effect == RuneEffect.INSIGHT)
            .mapToDouble(entry -> RuneSocketPolicy.experienceBonus(entry.tier)).sum();
    }

    private static List<Entry> entries(ItemStack item) {
        if (item == null || item.getType().isAir()) return List.of();
        var data = item.getPersistentDataContainer();
        String serialized = data.get(SOCKETS_KEY, PersistentDataType.STRING);
        if (serialized != null && !serialized.isBlank()) {
            List<Entry> result = new ArrayList<>();
            for (String rawEntry : serialized.split(",")) {
                String[] parts = rawEntry.split(":", 2);
                if (parts.length != 2) continue;
                RuneEffect.byId(parts[0]).ifPresent(effect -> {
                    try {
                        result.add(new Entry(effect, RuneTier.fromValue(Integer.parseInt(parts[1]))));
                    } catch (NumberFormatException ignored) {
                        // Ignore only the malformed socket; keep the remaining item data usable.
                    }
                });
            }
            return List.copyOf(result);
        }
        String legacyEffect = data.get(LEGACY_EFFECT_KEY, PersistentDataType.STRING);
        Integer legacyTier = data.get(LEGACY_TIER_KEY, PersistentDataType.INTEGER);
        if (legacyEffect == null || legacyTier == null) return List.of();
        return RuneEffect.byId(legacyEffect)
            .map(effect -> List.of(new Entry(effect, RuneTier.fromValue(legacyTier))))
            .orElseGet(List::of);
    }

    private static String serialize(List<Entry> entries) {
        return entries.stream().map(entry -> entry.effect.id() + ":" + entry.tier.value())
            .collect(java.util.stream.Collectors.joining(","));
    }

    private static void removeManagedLore(List<Component> lore) {
        for (int index = 0; index < lore.size(); index++) {
            String text = PLAIN.serialize(lore.get(index));
            if (text.startsWith("Sockety:") || text.startsWith("Runov\u00e9 sockety:")
                || text.startsWith("Nekara \u00b7 Vryt\u00e1 runa")) {
                int from = index > 0 && PLAIN.serialize(lore.get(index - 1)).isEmpty() ? index - 1 : index;
                lore.subList(from, lore.size()).clear();
                return;
            }
        }
    }
    private static NamedTextColor qualityColor(ItemStack item) {
        Integer quality = item.getPersistentDataContainer().get(QUALITY_KEY, PersistentDataType.INTEGER);
        return switch (quality == null ? 0 : quality) {
            case 1 -> NamedTextColor.WHITE;
            case 2 -> NamedTextColor.GREEN;
            case 3 -> NamedTextColor.BLUE;
            case 4 -> NamedTextColor.LIGHT_PURPLE;
            case 5 -> NamedTextColor.GOLD;
            default -> NamedTextColor.GRAY;
        };
    }

    private record Entry(RuneEffect effect, RuneTier tier) {
    }
}
