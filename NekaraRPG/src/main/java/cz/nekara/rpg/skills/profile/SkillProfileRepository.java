package cz.nekara.rpg.skills.profile;

import java.util.Optional;

public interface SkillProfileRepository extends AutoCloseable {
    Optional<SkillProfile> find(String playerKey);

    SkillProfile save(SkillProfile profile, long expectedRevision);

    @Override
    default void close() {
    }
}
