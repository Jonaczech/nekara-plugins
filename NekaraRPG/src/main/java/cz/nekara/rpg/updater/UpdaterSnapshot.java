package cz.nekara.rpg.updater;

import java.time.Instant;

public record UpdaterSnapshot(
        UpdaterState state,
        String currentVersion,
        String latestVersion,
        String detail,
        Instant checkedAt
) {
    public static UpdaterSnapshot initial(String currentVersion) {
        return new UpdaterSnapshot(UpdaterState.IDLE, currentVersion, null, "", null);
    }
}
