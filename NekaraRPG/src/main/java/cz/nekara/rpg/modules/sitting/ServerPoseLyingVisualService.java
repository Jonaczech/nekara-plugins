package cz.nekara.rpg.modules.sitting;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

final class ServerPoseLyingVisualService implements LyingVisualService {
    @Override
    public void show(Player subject, Collection<? extends Player> viewers) {
    }

    @Override
    public void show(Player subject, Player viewer) {
    }

    @Override
    public void hide(Player subject, Collection<? extends Player> viewers) {
    }

    @Override
    public void hide(Player subject, Player viewer) {
    }

    @Override
    public void forgetViewer(UUID viewerId) {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void close() {
    }
}
