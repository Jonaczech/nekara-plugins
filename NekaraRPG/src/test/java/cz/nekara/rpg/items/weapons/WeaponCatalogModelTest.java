package cz.nekara.rpg.items.weapons;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponCatalogModelTest {
    @Test
    void customDefinitionsUseStableIdsAndModelKeys() {
        WeaponDefinition dagger = WeaponCatalog.custom(WeaponFamily.DAGGER, WeaponTier.IRON).orElseThrow();
        WeaponDefinition hammer = WeaponCatalog.custom(WeaponFamily.HAMMER, WeaponTier.NETHERITE).orElseThrow();

        assertEquals("dagger/iron", dagger.id());
        assertEquals("weapons/dagger/iron", dagger.modelKey());
        assertEquals(Material.IRON_SWORD, dagger.material());
        assertEquals(Material.NETHERITE_AXE, hammer.material());
        assertTrue(dagger.custom());
    }

    @Test
    void vanillaFamiliesCannotBeRequestedAsCustomItems() {
        assertTrue(WeaponCatalog.custom(WeaponFamily.SWORD, WeaponTier.IRON).isEmpty());
        assertTrue(WeaponCatalog.custom(WeaponFamily.SPEAR, WeaponTier.IRON).isEmpty());
        assertTrue(WeaponCatalog.custom(WeaponFamily.AXE, WeaponTier.IRON).isEmpty());
        assertFalse(new WeaponDefinition(WeaponFamily.SWORD, WeaponTier.IRON, Material.IRON_SWORD).custom());
    }
}
