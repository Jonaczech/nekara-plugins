package cz.nekara.rpg.auth;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    @Test
    void hashesUseUniqueSaltsAndVerifyTheOriginalPassword() {
        PasswordHasher hasher = new PasswordHasher(1_000);
        char[] password = "spravne-heslo".toCharArray();

        String first = hasher.hash(password);
        String second = hasher.hash(password);

        assertNotEquals(first, second);
        assertTrue(hasher.verify(password, first));
        assertTrue(hasher.verify(password, second));
        assertFalse(hasher.verify("spatne-heslo".toCharArray(), first));
        Arrays.fill(password, '\0');
    }

    @Test
    void malformedOrUnknownHashesFailClosed() {
        PasswordHasher hasher = new PasswordHasher(1_000);

        assertFalse(hasher.verify("heslo".toCharArray(), null));
        assertFalse(hasher.verify("heslo".toCharArray(), "$SHA$legacy$hash"));
        assertFalse(hasher.verify("heslo".toCharArray(), "$PBKDF2-SHA256$abc$salt$hash"));
    }

    @Test
    void lowerWorkFactorIsMarkedForUpgrade() {
        PasswordHasher oldHasher = new PasswordHasher(1_000);
        PasswordHasher currentHasher = new PasswordHasher(2_000);
        String oldHash = oldHasher.hash("heslo-pro-upgrade".toCharArray());

        assertTrue(currentHasher.needsRehash(oldHash));
        assertFalse(oldHasher.needsRehash(oldHash));
    }
}
