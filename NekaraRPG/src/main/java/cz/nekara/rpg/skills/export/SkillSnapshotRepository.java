package cz.nekara.rpg.skills.export;

import java.io.IOException;
import java.nio.file.Path;

public interface SkillSnapshotRepository {
    void createConsistentSnapshot(Path target) throws IOException;
}
