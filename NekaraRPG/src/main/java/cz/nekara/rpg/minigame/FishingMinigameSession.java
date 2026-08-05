package cz.nekara.rpg.minigame;

import cz.nekara.rpg.configuration.MinigameConfig;
import cz.nekara.rpg.configuration.OutcomeEffectConfig;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public final class FishingMinigameSession {
    private final UUID playerId;
    private final FishHook hook;
    private final EquipmentSlot reelSlot;
    private final boolean testSession;
    private final MinigameEngine engine;
    private FishingSessionState state = FishingSessionState.MINIGAME_ACTIVE;
    private long lastInputMillis;
    private boolean reelRequested;
    private ItemStack deferredCatch;
    private Location deferredCatchLocation;
    private int vanillaExpToDrop;
    private OutcomeEffectConfig outcomeEffect;
    private int outcomeTicksRemaining;
    private BossBar progressBar;

    public FishingMinigameSession(
            UUID playerId,
            FishHook hook,
            EquipmentSlot reelSlot,
            boolean testSession,
            MinigameConfig config
    ) {
        this.playerId = playerId;
        this.hook = hook;
        this.reelSlot = reelSlot;
        this.testSession = testSession;
        this.engine = new MinigameEngine(config);
    }

    public UUID playerId() {
        return playerId;
    }

    public FishHook hook() {
        return hook;
    }

    public UUID hookId() {
        return hook == null ? null : hook.getUniqueId();
    }

    public EquipmentSlot reelSlot() {
        return reelSlot;
    }

    public boolean testSession() {
        return testSession;
    }

    public MinigameEngine engine() {
        return engine;
    }

    public FishingSessionState state() {
        return state;
    }

    public void state(FishingSessionState state) {
        this.state = state;
    }

    public boolean acceptsInput(long now, long debounceMillis) {
        if (lastInputMillis > 0 && now - lastInputMillis < debounceMillis) {
            return false;
        }
        lastInputMillis = now;
        return true;
    }

    public boolean reelRequested() {
        return reelRequested;
    }

    public void reelRequested(boolean reelRequested) {
        this.reelRequested = reelRequested;
    }

    public void deferCatch(Item item, int vanillaExpToDrop) {
        this.deferredCatch = item.getItemStack().clone();
        this.deferredCatchLocation = item.getLocation().clone();
        this.vanillaExpToDrop = Math.max(0, vanillaExpToDrop);
    }

    public boolean hasDeferredCatch() {
        return deferredCatch != null;
    }

    public ItemStack takeDeferredCatch() {
        ItemStack result = deferredCatch;
        deferredCatch = null;
        return result;
    }

    public Location deferredCatchLocation() {
        return deferredCatchLocation;
    }

    public int vanillaExpToDrop() {
        return vanillaExpToDrop;
    }


    public void beginOutcomeEffect(OutcomeEffectConfig effect) {
        this.outcomeEffect = effect;
        this.outcomeTicksRemaining = effect != null && effect.enabled() && hook != null
                ? effect.durationTicks() : 0;
    }

    public OutcomeEffectConfig outcomeEffect() {
        return outcomeEffect;
    }

    public int outcomeTicksRemaining() {
        return outcomeTicksRemaining;
    }

    public void decrementOutcomeTicks() {
        outcomeTicksRemaining = Math.max(0, outcomeTicksRemaining - 1);
    }

    public BossBar progressBar() {
        return progressBar;
    }

    public void progressBar(BossBar progressBar) {
        this.progressBar = progressBar;
    }

    public void removeProgressBar() {
        if (progressBar != null) {
            progressBar.removeAll();
            progressBar.setVisible(false);
            progressBar = null;
        }
    }
}
