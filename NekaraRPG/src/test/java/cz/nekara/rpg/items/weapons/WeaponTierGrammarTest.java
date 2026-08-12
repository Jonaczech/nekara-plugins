package cz.nekara.rpg.items.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WeaponTierGrammarTest {
    @Test
    void materialPrefixesAgreeWithTheWeaponNounGender() {
        assertEquals("\u017delezn\u00fd", WeaponTier.IRON.displayPrefix(WeaponFamily.SWORD));
        assertEquals("\u017delezn\u00e1", WeaponTier.IRON.displayPrefix(WeaponFamily.DAGGER));
        assertEquals("\u017delezn\u00e9", WeaponTier.IRON.displayPrefix(WeaponFamily.HAMMER));
        assertEquals("M\u011bd\u011bn\u00e1", WeaponTier.COPPER.displayPrefix(WeaponFamily.AXE));
        assertEquals("Diamantov\u00e9", WeaponTier.DIAMOND.displayPrefix(WeaponFamily.SPEAR));
    }
}