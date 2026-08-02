package cz.nekara.rpg.mount;

import org.bukkit.entity.Horse;
import org.bukkit.configuration.file.YamlConfiguration;
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

class YamlMountRepositoryTest {
    @TempDir
    File temporaryDirectory;

    @Test
    void mountStateAndCombatWindowSurviveRepositoryReload() throws Exception {
        File storage = new File(temporaryDirectory, "mounts.yml");
        YamlMountRepository repository = new YamlMountRepository(storage);
        UUID ownerUuid = UUID.randomUUID();
        UUID mountId = UUID.randomUUID();
        UUID entityUuid = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-02T12:00:00Z");
        MountRecord mount = new MountRecord(
                "name:hrac", "Hrac", ownerUuid, mountId, entityUuid, "Stín",
                17.5, 30.0, 0.225, 0.72, Horse.Color.BLACK, Horse.Style.WHITE_DOTS,
                null, null, 40, 20, 200, List.of(), now.plusSeconds(30), null, null, now);

        assertTrue(repository.create(mount));
        assertFalse(repository.create(mount));
        repository.setCombatUntil(Map.of(mount.ownerId(), now.plusSeconds(15)));
        repository.update(mount.dormant(now.plusSeconds(1)));

        YamlMountRepository reloaded = new YamlMountRepository(storage);
        MountRecord stored = reloaded.findByOwnerId(mount.ownerId()).orElseThrow();
        assertEquals(mount.mountId(), stored.mountId());
        assertEquals("Stín", stored.customName());
        assertEquals(17.5, stored.health(), 0.0001);
        assertEquals(Horse.Color.BLACK, stored.color());
        assertEquals(Horse.Style.WHITE_DOTS, stored.style());
        assertEquals(now.plusSeconds(30), stored.summonAvailableAt());
        assertEquals(null, stored.activeEntityUuid());
        assertEquals(now.plusSeconds(15), reloaded.combatUntil(mount.ownerId()).orElseThrow());
    }

    @Test
    void legacyRecordWithoutCustomNameIsMigratedBeforeGuiCanReadIt() throws Exception {
        File storage = new File(temporaryDirectory, "legacy-mounts.yml");
        YamlMountRepository repository = new YamlMountRepository(storage);
        Instant now = Instant.parse("2026-08-02T12:00:00Z");
        MountRecord mount = new MountRecord(
                "name:hrac", "Hrac", UUID.randomUUID(), UUID.randomUUID(), null, "Stín",
                30.0, 30.0, 0.225, 0.72, Horse.Color.BLACK, Horse.Style.NONE,
                null, null, 0, 0, 300, List.of(), null, null, null, now);
        assertTrue(repository.create(mount));

        YamlConfiguration legacy = new YamlConfiguration();
        legacy.load(storage);
        legacy.set("mounts.name:hrac.custom-name", null);
        legacy.save(storage);

        YamlMountRepository migrated = new YamlMountRepository(storage);
        assertEquals(YamlMountRepository.LEGACY_MOUNT_NAME,
                migrated.findByOwnerId(mount.ownerId()).orElseThrow().customName());

        YamlConfiguration persisted = new YamlConfiguration();
        persisted.load(storage);
        assertEquals(YamlMountRepository.LEGACY_MOUNT_NAME,
                persisted.getString("mounts.name:hrac.custom-name"));
    }
}
