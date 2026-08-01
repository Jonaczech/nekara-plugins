package cz.nekara.rpg.fishing;

public final class DeferredCatchCompatibilityStrategy implements FishingCompatibilityStrategy {
    @Override
    public String modeName() {
        return "DEFERRED_CATCH";
    }
}
