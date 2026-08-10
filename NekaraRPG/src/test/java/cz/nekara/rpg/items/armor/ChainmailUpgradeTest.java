package cz.nekara.rpg.items.armor;

import java.util.Arrays;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChainmailUpgradeTest {
    @Test
    void definesOneLeatherAndIronUpgradeForEveryArmorSlot() {
        assertEquals(Set.of(
            Material.CHAINMAIL_HELMET,
            Material.CHAINMAIL_CHESTPLATE,
            Material.CHAINMAIL_LEGGINGS,
            Material.CHAINMAIL_BOOTS
        ), Arrays.stream(ChainmailUpgrade.values())
            .map(ChainmailUpgrade::chainmailResult)
            .collect(java.util.stream.Collectors.toSet()));
        for (ChainmailUpgrade upgrade : ChainmailUpgrade.values()) {
            assertEquals("LEATHER_" + upgrade.chainmailResult().name().substring("CHAINMAIL_".length()),
                upgrade.leatherBase().name());
        }
    }
}
