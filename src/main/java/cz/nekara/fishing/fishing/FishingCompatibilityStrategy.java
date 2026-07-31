package cz.nekara.fishing.fishing;

/** Compatibility boundary for future fishing modes without coupling the minigame to server internals. */
public interface FishingCompatibilityStrategy {
    String modeName();
}
