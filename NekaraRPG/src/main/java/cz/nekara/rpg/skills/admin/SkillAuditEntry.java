package cz.nekara.rpg.skills.admin;

public record SkillAuditEntry(
    long id,
    SkillAdminActor actor,
    String targetPlayerKey,
    String targetDisplayName,
    String operation,
    String detail,
    long occurredAtEpochMillis,
    long revisionBefore,
    long revisionAfter
) {
}
