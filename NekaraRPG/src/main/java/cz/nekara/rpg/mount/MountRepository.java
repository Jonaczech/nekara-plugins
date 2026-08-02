package cz.nekara.rpg.mount;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MountRepository {
    Optional<MountRecord> findByOwnerId(String ownerId);

    Optional<MountRecord> findByMountId(UUID mountId);

    Collection<MountRecord> findAll();

    boolean create(MountRecord mount) throws IOException;

    void update(MountRecord mount) throws IOException;

    Optional<Instant> combatUntil(String ownerId);

    void setCombatUntil(Map<String, Instant> combatWindows) throws IOException;
}
