package cz.nekara.rpg.mount;

import cz.nekara.rpg.mount.ActiveMountCoordinator.MountKind;
import org.junit.jupiter.api.Test;

import static cz.nekara.rpg.mount.ActiveMountSwitchPolicy.Decision.ACTIVATE;
import static cz.nekara.rpg.mount.ActiveMountSwitchPolicy.Decision.BLOCK_PASSENGERS;
import static cz.nekara.rpg.mount.ActiveMountSwitchPolicy.Decision.DEACTIVATE_AND_ACTIVATE;
import static cz.nekara.rpg.mount.ActiveMountSwitchPolicy.Decision.KEEP_ACTIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveMountSwitchPolicyTest {
    @Test
    void onlyTheOtherUnriddenMountMustBeDeactivated() {
        assertEquals(ACTIVATE, ActiveMountSwitchPolicy.decide(null, MountKind.DRAGON, false));
        assertEquals(KEEP_ACTIVE, ActiveMountSwitchPolicy.decide(MountKind.HORSE, MountKind.HORSE, true));
        assertEquals(BLOCK_PASSENGERS,
                ActiveMountSwitchPolicy.decide(MountKind.HORSE, MountKind.DRAGON, true));
        assertEquals(DEACTIVATE_AND_ACTIVATE,
                ActiveMountSwitchPolicy.decide(MountKind.HORSE, MountKind.DRAGON, false));
    }
}
