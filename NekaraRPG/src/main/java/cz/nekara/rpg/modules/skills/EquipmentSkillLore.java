package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.items.weapons.WeaponCatalog;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillPresentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Maintains the concise owning-skill line for combat equipment. */
public final class EquipmentSkillLore {
    private static final String PREFIX = "Dovednost: ";
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private EquipmentSkillLore() {
    }

    public static void refresh(ItemStack item) {
        Optional<SkillId> skill = skillFor(item);
        if (skill.isEmpty()) return;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.removeIf(line -> PLAIN.serialize(line).startsWith(PREFIX));
        lore.add(0, Component.text(PREFIX + SkillPresentation.czechName(skill.get()), NamedTextColor.AQUA));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    static Optional<SkillId> skillFor(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        Optional<SkillId> weaponSkill = WeaponCatalog.resolve(item).map(definition -> definition.family().skill());
        if (weaponSkill.isPresent()) return weaponSkill;
        String name = item.getType().name();
        if (!(name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
            || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS"))) {
            return Optional.empty();
        }
        if (name.startsWith("LEATHER_") || name.startsWith("CHAINMAIL_") || name.startsWith("DIAMOND_")) {
            return Optional.of(SkillId.LIGHT_ARMOR);
        }
        if (name.startsWith("COPPER_") || name.startsWith("IRON_")
            || name.startsWith("GOLDEN_") || name.startsWith("NETHERITE_")) {
            return Optional.of(SkillId.HEAVY_ARMOR);
        }
        return Optional.empty();
    }
}