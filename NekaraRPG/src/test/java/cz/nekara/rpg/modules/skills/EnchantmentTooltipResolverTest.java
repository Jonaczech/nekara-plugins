package cz.nekara.rpg.modules.skills;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantmentTooltipResolverTest {
    @Test
    void explainsNumericalVanillaEnchantments() {
        assertEquals(List.of("+2.5 poškození při zásahu živého cíle."),
            EnchantmentTooltipResolver.explain("minecraft:sharpness", 4));
        assertEquals(List.of("48 % ochrany proti pádu (společný limit 80 %)."),
            EnchantmentTooltipResolver.explain("minecraft:feather_falling", 4));
        assertEquals(List.of("Zapálí cíl na 8 s."),
            EnchantmentTooltipResolver.explain("minecraft:fire_aspect", 2));
    }

    @Test
    void keepsUnknownEnchantmentsReadable() {
        assertEquals(List.of("Vanilla účinek, úroveň 2."),
            EnchantmentTooltipResolver.explain("minecraft:future_enchantment", 2));
    }
}
