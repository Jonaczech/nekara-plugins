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
}
