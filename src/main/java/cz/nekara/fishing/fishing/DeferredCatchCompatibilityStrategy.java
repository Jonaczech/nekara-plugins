package cz.nekara.fishing.fishing;

public final class DeferredCatchCompatibilityStrategy implements FishingCompatibilityStrategy {
    @Override
    public String modeName() {
        return "DEFERRED_CATCH";
    }
}
