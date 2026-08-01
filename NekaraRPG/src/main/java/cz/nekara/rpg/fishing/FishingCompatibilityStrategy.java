package cz.nekara.rpg.fishing;

/** Compatibility boundary for future fishing modes without coupling the minigame to server internals. */
public interface FishingCompatibilityStrategy {
    String modeName();
}
