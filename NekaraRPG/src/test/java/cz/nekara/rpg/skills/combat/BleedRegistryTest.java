package cz.nekara.rpg.skills.combat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BleedRegistryTest {
    @Test
    void oneCentralRegistryAdvancesAndExpiresBleeds() {
        BleedRegistry registry = new BleedRegistry(10);
        UUID target = UUID.randomUUID();
        UUID source = UUID.randomUUID();

        assertTrue(registry.apply(target, source, 1.5, 3));
        assertEquals(1.5, registry.advance().getFirst().damage());
        assertEquals(1, registry.size());
        registry.advance();
        registry.advance();
        assertEquals(0, registry.size());
    }

    @Test
    void strongerBleedRefreshesWhileCapacityRemainsBounded() {
        BleedRegistry registry = new BleedRegistry(1);
        UUID target = UUID.randomUUID();
        UUID source = UUID.randomUUID();

        assertTrue(registry.apply(target, source, 1.0, 2));
        assertTrue(registry.apply(target, source, 2.0, 3));
        assertFalse(registry.apply(UUID.randomUUID(), source, 5.0, 3));
        assertEquals(2.0, registry.advance().getFirst().damage());
        assertEquals(1, registry.size());
    }
}
