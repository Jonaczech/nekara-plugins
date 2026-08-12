package cz.nekara.rpg.modules.runes;

import java.util.Arrays;
import java.util.Optional;
import org.bukkit.Material;

enum RuneEffect {
    EMBER("ember", "Runa žáru", Material.RED_DYE, RuneTarget.WEAPON, "Zásah zapálí cíl na %s s.", false),
    TRACKER("tracker", "Runa stopaře", Material.BLUE_DYE, RuneTarget.BOW, "Zásah šípem označí cíl na %s s.", false),
    STEADFAST("steadfast", "Runa pevného kroku", Material.WHITE_DYE, RuneTarget.BOOTS, "Sníží poškození pádem o %s %%.", false),
    PRESERVATION("preservation", "Runa šetrné práce", Material.GREEN_DYE, RuneTarget.TOOL, "%s %% šance neztratit odolnost nástroje.", false),
    INSIGHT("insight", "Runa poznání", Material.WHITE_DYE, RuneTarget.EQUIPMENT, "Zisk XP do dovedností: +%s %%.", true);

    private final String id;
    private final String displayName;
    private final Material dye;
    private final RuneTarget target;
    private final String description;
    private final boolean craftable;

    RuneEffect(String id, String displayName, Material dye, RuneTarget target, String description, boolean craftable) {
        this.id = id;
        this.displayName = displayName;
        this.dye = dye;
        this.target = target;
        this.description = description;
        this.craftable = craftable;
    }

    String id() { return id; }
    String displayName() { return displayName; }
    Material dye() { return dye; }
    RuneTarget target() { return target; }
    String description(RuneTier tier) { return description.formatted(value(tier)); }

    int value(RuneTier tier) {
        return switch (this) {
            case EMBER -> tier.value();
            case TRACKER -> tier.value() * 4;
            case STEADFAST, PRESERVATION -> tier.value() * 10;
            case INSIGHT -> switch (tier) {
                case I -> 1;
                case II -> 3;
                case III -> 5;
            };
        };
    }

    static Optional<RuneEffect> byDye(Material material) {
        return Arrays.stream(values()).filter(effect -> effect.craftable && effect.dye == material).findFirst();
    }

    static Optional<RuneEffect> byId(String id) {
        return Arrays.stream(values()).filter(effect -> effect.id.equals(id)).findFirst();
    }
}
