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
        assertEquals(Material.MACE, hammer.material());
        assertTrue(dagger.custom());
    }

    @Test
    void ironCustomWeaponsUseTheResourcePackModelKeys() {
        assertEquals("weapons/dagger/iron", WeaponCatalog.custom(WeaponFamily.DAGGER, WeaponTier.IRON)
            .orElseThrow().modelKey());
        assertEquals("weapons/greatsword/iron", WeaponCatalog.custom(WeaponFamily.GREATSWORD, WeaponTier.IRON)
            .orElseThrow().modelKey());
        assertEquals("weapons/hammer/iron", WeaponCatalog.custom(WeaponFamily.HAMMER, WeaponTier.IRON)
            .orElseThrow().modelKey());
    }

    @Test
    void everyNonWoodenCustomWeaponHasTheExpectedModelKey() {
        for (WeaponFamily family : new WeaponFamily[]{WeaponFamily.DAGGER, WeaponFamily.GREATSWORD, WeaponFamily.HAMMER}) {
            for (WeaponTier tier : WeaponTier.values()) {
                if (tier == WeaponTier.WOODEN) {
                    continue;
                }
                assertEquals("weapons/" + family.name().toLowerCase() + "/" + tier.id(),
                    WeaponCatalog.custom(family, tier).orElseThrow().modelKey());
            }
        }
    }

    @Test
    void woodenCustomWeaponsAreUnavailable() {
        for (WeaponFamily family : new WeaponFamily[]{WeaponFamily.DAGGER, WeaponFamily.GREATSWORD, WeaponFamily.HAMMER}) {
            assertTrue(WeaponCatalog.custom(family, WeaponTier.WOODEN).isEmpty());
        }
    }

    @Test
    void diamondCustomWeaponsUpgradeToTheirNetheriteFamilyVariant() {
        for (WeaponFamily family : new WeaponFamily[]{WeaponFamily.DAGGER, WeaponFamily.GREATSWORD, WeaponFamily.HAMMER}) {
            WeaponDefinition diamond = WeaponCatalog.custom(family, WeaponTier.DIAMOND).orElseThrow();
            WeaponDefinition netherite = WeaponCatalog.netheriteUpgrade(diamond).orElseThrow();
            assertEquals(WeaponTier.NETHERITE, netherite.tier());
            assertEquals(family, netherite.family());
        }
        assertTrue(WeaponCatalog.netheriteUpgrade(new WeaponDefinition(
            WeaponFamily.SWORD, WeaponTier.DIAMOND, Material.DIAMOND_SWORD)).isEmpty());
    }

    @Test
    void vanillaFamiliesCannotBeRequestedAsCustomItems() {
        assertTrue(WeaponCatalog.custom(WeaponFamily.SWORD, WeaponTier.IRON).isEmpty());
        assertTrue(WeaponCatalog.custom(WeaponFamily.SPEAR, WeaponTier.IRON).isEmpty());
        assertTrue(WeaponCatalog.custom(WeaponFamily.AXE, WeaponTier.IRON).isEmpty());
        assertFalse(new WeaponDefinition(WeaponFamily.SWORD, WeaponTier.IRON, Material.IRON_SWORD).custom());
    }
}
