package cz.nekara.fishing.minigame;

public enum FishingSessionState {
    WAITING_FOR_BITE,
    MINIGAME_ACTIVE,
    WAITING_FOR_REEL,
    CATCH_COMPLETED,
    FAILED,
    CANCELLED
}
