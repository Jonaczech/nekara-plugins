package cz.nekara.rpg.mount;

import org.bukkit.entity.Horse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteMountRepositoryTest {
    @TempDir File temporaryDirectory;

    @Test
    void recordsAndCombatWindowsSurviveTransactionalReload() throws Exception {
        File database = new File(temporaryDirectory, "mounts.db");
        Instant now = Instant.parse("2026-08-03T12:00:00Z");
        MountRecord mount = mount(now);
        try (SqliteMountRepository repository = new SqliteMountRepository(database)) {
            assertTrue(repository.isEmpty());
            assertTrue(repository.create(mount));
            assertFalse(repository.create(mount));
            repository.update(mount.dormant(now.plusSeconds(1)));
            repository.setCombatUntil(Map.of(mount.ownerId(), now.plusSeconds(15)));
        }
        try (SqliteMountRepository repository = new SqliteMountRepository(database)) {
            MountRecord stored = repository.findByMountId(mount.mountId()).orElseThrow();
            assertEquals(mount.ownerId(), stored.ownerId());
            assertEquals("StĂ„â€šĂ‚Â­n", stored.customName());
            assertEquals(null, stored.activeEntityUuid());
            assertEquals(now.plusSeconds(15), repository.combatUntil(mount.ownerId()).orElseThrow());
            assertEquals(repository.combatWindows().get(mount.ownerId()), now.plusSeconds(15));
        }
    }

    @Test
    void yamlRecordsCanBeImportedOnce() throws Exception {
        Instant now = Instant.parse("2026-08-03T12:00:00Z");
        MountRecord mount = mount(now);
        try (SqliteMountRepository repository = new SqliteMountRepository(
                new File(temporaryDirectory, "import.db"))) {
            repository.importAll(List.of(mount), Map.of(mount.ownerId(), now.plusSeconds(5)));
            assertFalse(repository.isEmpty());
            assertTrue(repository.isLegacyMigrationComplete());
            assertEquals(mount.mountId(), repository.findByOwnerId(mount.ownerId()).orElseThrow().mountId());
        }
    }

    private MountRecord mount(Instant now) {
        return new MountRecord(
                "name:hrac", "Hrac", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "StĂ„â€šĂ‚Â­n",
                17.5, 30.0, 0.225, 0.72, Horse.Color.BLACK, Horse.Style.WHITE_DOTS,
                null, null, null, List.of(), 40, 20, 200, List.of(),
                now.plusSeconds(30), null, null, now);
    }
}
