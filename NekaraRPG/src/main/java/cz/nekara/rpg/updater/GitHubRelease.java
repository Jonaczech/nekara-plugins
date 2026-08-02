package cz.nekara.rpg.updater;

import java.net.URI;

public record GitHubRelease(
        SemanticVersion version,
        URI downloadUri,
        long assetSize,
        String sha256
) {
}
