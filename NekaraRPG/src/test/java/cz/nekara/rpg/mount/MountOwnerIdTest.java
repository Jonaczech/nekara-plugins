package cz.nekara.rpg.mount;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MountOwnerIdTest {
    @Test
    void ownerIdentityIsCaseStableForOfflineMode() {
        assertEquals("name:hrac_01", MountOwnerId.fromPlayerName("HrAc_01"));
        assertEquals(MountOwnerId.fromPlayerName("HRAC_01"), MountOwnerId.fromPlayerName("hrac_01"));
    }

    @Test
    void emptyOwnerCannotBePersisted() {
        assertThrows(IllegalArgumentException.class, () -> MountOwnerId.fromPlayerName("   "));
    }
}
