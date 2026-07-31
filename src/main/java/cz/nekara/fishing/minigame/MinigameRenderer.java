package cz.nekara.fishing.minigame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Locale;

public final class MinigameRenderer {
    private static final String TRACK_CELL = "\u2501";
    private static final String TARGET_CELL = "\u2501";
    private static final String INDICATOR_CELL = "\u2693";

    public Component render(MinigameEngine engine) {
        TextComponent.Builder result = Component.text();

        for (int index = 0; index < engine.config().barLength(); index++) {
            if (index == engine.indicatorPosition()) {
                result.append(Component.text(INDICATOR_CELL, NamedTextColor.GOLD));
            } else if (index >= engine.targetStart() && index < engine.targetEndExclusive()) {
                result.append(Component.text(TARGET_CELL, NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD));
            } else {
                result.append(Component.text(TRACK_CELL, NamedTextColor.DARK_GRAY));
            }
        }

        result.append(Component.text("  "));
        result.append(Component.text(String.format(Locale.ROOT, "%.1fs", engine.timeLeftTicks() / 20.0),
                engine.timeLeftTicks() <= 40 ? NamedTextColor.RED : NamedTextColor.AQUA));
        return result.build();
    }
}
