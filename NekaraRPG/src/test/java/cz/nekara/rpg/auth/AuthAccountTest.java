package cz.nekara.rpg.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthAccountTest {
    @Test
    void nicknameNormalizationIsLocaleIndependentAndCaseInsensitive() {
        assertEquals("jonaczech_1", AuthAccount.normalize("JonaCzech_1"));
    }

    @Test
    void yamlUnsafeOrProtocolInvalidNicknamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AuthAccount(
                "player.name", "player.name", UUID.randomUUID(), "hash",
                Instant.EPOCH, null));
        assertThrows(IllegalArgumentException.class, () -> new AuthAccount(
                "name/section", "name/section", UUID.randomUUID(), "hash",
                Instant.EPOCH, null));
    }

    @Test
    void passwordReplacementPreservesAccountIdentityAndAuditMetadata() {
        UUID uuid = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant lastLoginAt = Instant.parse("2026-01-02T10:00:00Z");
        AuthAccount original = new AuthAccount(
                "JonaCzech_1", "jonaczech_1", uuid, "old-hash", createdAt, lastLoginAt);

        AuthAccount updated = original.withPasswordHash("new-hash");

        assertEquals("new-hash", updated.passwordHash());
        assertEquals(original.username(), updated.username());
        assertEquals(original.normalizedUsername(), updated.normalizedUsername());
        assertEquals(uuid, updated.lastKnownUuid());
        assertEquals(createdAt, updated.createdAt());
        assertEquals(lastLoginAt, updated.lastLoginAt());
    }
}
