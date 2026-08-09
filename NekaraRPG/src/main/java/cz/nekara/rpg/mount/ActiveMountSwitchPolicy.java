package cz.nekara.rpg.mount;

import cz.nekara.rpg.mount.ActiveMountCoordinator.MountKind;

public final class ActiveMountSwitchPolicy {
    private ActiveMountSwitchPolicy() {
    }

    public static Decision decide(MountKind activeKind, MountKind requestedKind, boolean hasPassengers) {
        if (activeKind == null) {
            return Decision.ACTIVATE;
        }
        if (activeKind == requestedKind) {
            return Decision.KEEP_ACTIVE;
        }
        return hasPassengers ? Decision.BLOCK_PASSENGERS : Decision.DEACTIVATE_AND_ACTIVATE;
    }

    public enum Decision {
        ACTIVATE,
        KEEP_ACTIVE,
        BLOCK_PASSENGERS,
        DEACTIVATE_AND_ACTIVATE
    }
}
