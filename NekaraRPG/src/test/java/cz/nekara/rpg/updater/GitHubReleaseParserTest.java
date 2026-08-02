package cz.nekara.rpg.updater;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubReleaseParserTest {
    private static final String DIGEST = "a".repeat(64);

    @Test
    void parsesTheSingleStableNekaraArtifact() {
        GitHubRelease release = new GitHubReleaseParser().parse(releaseJson(
                "v1.2.0", "NekaraRPG.jar", "sha256:" + DIGEST,
                "https://github.com/Jonaczech/nekara-plugins/releases/download/v1.2.0/NekaraRPG.jar",
                false, false));

        assertEquals(new SemanticVersion(1, 2, 0), release.version());
        assertEquals(130_000L, release.assetSize());
        assertEquals(DIGEST, release.sha256());
    }

    @Test
    void rejectsPrereleasesMissingDigestsAndUntrustedDownloads() {
        GitHubReleaseParser parser = new GitHubReleaseParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(releaseJson(
                "v1.2.0", "NekaraRPG.jar", "sha256:" + DIGEST,
                "https://github.com/Jonaczech/nekara-plugins/releases/download/v1.2.0/NekaraRPG.jar",
                false, true)));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(releaseJson(
                "v1.2.0", "NekaraRPG.jar", "",
                "https://github.com/Jonaczech/nekara-plugins/releases/download/v1.2.0/NekaraRPG.jar",
                false, false)));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(releaseJson(
                "v1.2.0", "NekaraRPG.jar", "sha256:" + DIGEST,
                "https://example.com/NekaraRPG.jar", false, false)));
    }

    @Test
    void rejectsWrongOrDuplicateAssetNames() {
        GitHubReleaseParser parser = new GitHubReleaseParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parse(releaseJson(
                "v1.2.0", "NekaraRPG-1.2.0.jar", "sha256:" + DIGEST,
                "https://github.com/Jonaczech/nekara-plugins/releases/download/v1.2.0/NekaraRPG-1.2.0.jar",
                false, false)));

        String duplicate = """
                {
                  "tag_name": "v1.2.0",
                  "draft": false,
                  "prerelease": false,
                  "assets": [
                    {
                      "name": "NekaraRPG.jar",
                      "size": 130000,
                      "digest": "sha256:%s",
                      "browser_download_url": "https://github.com/Jonaczech/nekara-plugins/releases/download/v1.2.0/NekaraRPG.jar"
                    },
                    {
                      "name": "NekaraRPG.jar",
                      "size": 130000,
                      "digest": "sha256:%s",
                      "browser_download_url": "https://github.com/Jonaczech/nekara-plugins/releases/download/v1.2.0/NekaraRPG.jar"
                    }
                  ]
                }
                """.formatted(DIGEST, DIGEST);
        assertThrows(IllegalArgumentException.class, () -> parser.parse(duplicate));
    }

    private String releaseJson(
            String tag,
            String assetName,
            String digest,
            String downloadUrl,
            boolean draft,
            boolean prerelease
    ) {
        return """
                {
                  "tag_name": "%s",
                  "draft": %s,
                  "prerelease": %s,
                  "assets": [{
                    "name": "%s",
                    "size": 130000,
                    "digest": "%s",
                    "browser_download_url": "%s"
                  }]
                }
                """.formatted(tag, draft, prerelease, assetName, digest, downloadUrl);
    }
}
