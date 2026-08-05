package cz.nekara.rpg.items.weapons;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class WeaponRecipePatternTest {
    @Test
    void hammerUsesTheCrossHeadPatternInsteadOfThePickaxePattern() {
        assertArrayEquals(new String[] { " A ", "ASA", " S " },
            WeaponRecipePattern.forFamily(WeaponFamily.HAMMER).rows());
    }

    @Test
    void greatswordUsesAThreeWideBladeWithAReinforcedGrip() {
        assertArrayEquals(new String[] { "AAA", " B ", " S " },
            WeaponRecipePattern.forFamily(WeaponFamily.GREATSWORD).rows());
    }
}
