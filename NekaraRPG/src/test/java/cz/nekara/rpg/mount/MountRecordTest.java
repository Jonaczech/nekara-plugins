package cz.nekara.rpg.mount;

import org.bukkit.entity.Horse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountRecordTest {
    private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");

    @Test
    void dismissalPreservesStateAndDeathSurvivesUntilExplicitRevival() {
        MountRecord active = record(18.0, 30.0, UUID.randomUUID());

        MountRecord dormant = active.dormant(NOW.plusSeconds(1));
        assertNull(dormant.activeEntityUuid());
        assertEquals(18.0, dormant.health(), 0.0001);
        assertEquals(80, dormant.fireTicks());

        Instant summonAvailableAt = NOW.plusSeconds(30);
        dormant = dormant.withSummonAvailableAt(summonAvailableAt, NOW.plusSeconds(1));

        MountRecord dead = dormant.killed(NOW.plusSeconds(2), NOW.plusSeconds(62));
        assertTrue(dead.isDead());
        assertEquals(0.0, dead.health(), 0.0001);
        assertEquals(NOW.plusSeconds(62), dead.reviveAt());
        assertEquals(summonAvailableAt, dead.summonAvailableAt());

        MountRecord revived = dead.revived(NOW.plusSeconds(63));
        assertFalse(revived.isDead());
        assertEquals(30.0, revived.health(), 0.0001);
        assertEquals(80, revived.fireTicks());
        assertEquals(summonAvailableAt, revived.summonAvailableAt());
    }

    @Test
    void invalidPersistedAttributesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> record(31.0, 30.0, UUID.randomUUID()));
    }

    @Test
    void missingNameIsRejectedAtTheDomainBoundary() {
        MountRecord valid = record(30.0, 30.0, null);
        assertThrows(NullPointerException.class, () -> new MountRecord(
                valid.ownerId(), valid.ownerName(), valid.lastKnownOwnerUuid(), valid.mountId(), null,
                null, valid.health(), valid.maxHealth(), valid.movementSpeed(), valid.jumpStrength(),
                valid.color(), valid.style(), null, null, 0, 0, 300, List.of(),
                null, null, null, NOW));
    }

    private MountRecord record(double health, double maxHealth, UUID entityUuid) {
        return new MountRecord("name:hrac", "Hrac", UUID.randomUUID(), UUID.randomUUID(),
                entityUuid, "Stín", health, maxHealth, 0.225, 0.72,
                Horse.Color.BLACK, Horse.Style.WHITE_DOTS, null, null,
                80, 20, 200, List.of(), null, null, null, NOW);
    }
}
