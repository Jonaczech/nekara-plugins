package cz.nekara.rpg.skills.admin;

import java.util.Objects;

public record SkillAuditRecord(
    SkillAdminActor actor,
    String targetDisplayName,
    String operation,
    String detail,
    long occurredAtEpochMillis
) {
    public SkillAuditRecord {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(targetDisplayName, "targetDisplayName");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(detail, "detail");
        if (targetDisplayName.isBlank() || operation.isBlank() || detail.isBlank()) {
            throw new IllegalArgumentException("Administrative audit values cannot be blank");
        }
        if (occurredAtEpochMillis < 0) {
            throw new IllegalArgumentException("Administrative audit time cannot be negative");
        }
    }
}
