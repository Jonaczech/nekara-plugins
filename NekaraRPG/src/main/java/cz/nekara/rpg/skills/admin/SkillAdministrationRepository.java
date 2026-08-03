package cz.nekara.rpg.skills.admin;

import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProfileRepository;
import java.util.List;

public interface SkillAdministrationRepository extends SkillProfileRepository {
    SkillProfile saveAdminMutation(
        SkillProfile profile,
        long expectedRevision,
        SkillAuditRecord auditRecord
    );

    List<SkillAuditEntry> findRecentAuditEntries(String playerKey, int limit);
}
