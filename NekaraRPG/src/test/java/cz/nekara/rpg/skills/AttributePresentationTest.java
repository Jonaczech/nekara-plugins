package cz.nekara.rpg.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AttributePresentationTest {
    @Test
    void formatsRatiosAsReadableCzechPercentages() {
        assertEquals("12,5 %", AttributePresentation.percentage(0.125));
        assertEquals("+20 %", AttributePresentation.bonusPercentage(1.2));
        assertEquals("-5 %", AttributePresentation.signedPercentage(-0.05));
    }

    @Test
    void omitsUnnecessaryDecimalPlaces() {
        assertEquals("20", AttributePresentation.decimal(20.0));
        assertEquals("20,5", AttributePresentation.decimal(20.5));
    }
}
