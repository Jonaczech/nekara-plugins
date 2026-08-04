package cz.nekara.rpg.skills.export;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record SkillExportResult(
    Path archive,
    Instant createdAt,
    long profileCount,
    long sizeBytes,
    String sha256
) {
    public SkillExportResult {
        Objects.requireNonNull(archive, "archive");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(sha256, "sha256");
        if (profileCount < 0 || sizeBytes < 0 || sha256.length() != 64) {
            throw new IllegalArgumentException("Invalid skill export result");
        }
    }
}
