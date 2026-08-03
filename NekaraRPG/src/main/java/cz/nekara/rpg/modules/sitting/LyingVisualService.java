package cz.nekara.rpg.modules.sitting;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

interface LyingVisualService extends AutoCloseable {
    void show(Player subject, Collection<? extends Player> viewers);

    void show(Player subject, Player viewer);

    void hide(Player subject, Collection<? extends Player> viewers);

    void hide(Player subject, Player viewer);

    void forgetViewer(UUID viewerId);

    boolean isAvailable();

    @Override
    void close();
}
