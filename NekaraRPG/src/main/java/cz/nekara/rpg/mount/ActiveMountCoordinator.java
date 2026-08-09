package cz.nekara.rpg.mount;

import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Enforces the one-active-mount rule across independently implemented mount modules. */
public final class ActiveMountCoordinator {
    private final Map<UUID, ActiveMount> activeByOwner = new HashMap<>();

    public ActivationResult prepareActivation(UUID ownerId, MountKind requestedKind) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(requestedKind, "requestedKind");
        ActiveMount current = activeByOwner.get(ownerId);
        if (current == null) return ActivationResult.READY;
        if (!current.entity().isValid()) {
            activeByOwner.remove(ownerId, current);
            return ActivationResult.READY;
        }
        ActiveMountSwitchPolicy.Decision decision = ActiveMountSwitchPolicy.decide(
                current.kind(), requestedKind, !current.entity().getPassengers().isEmpty());
        if (decision == ActiveMountSwitchPolicy.Decision.KEEP_ACTIVE) return ActivationResult.ALREADY_ACTIVE;
        if (decision == ActiveMountSwitchPolicy.Decision.BLOCK_PASSENGERS) return ActivationResult.HAS_PASSENGERS;
        if (!current.deactivate().getAsBoolean()) {
            return ActivationResult.DEACTIVATION_FAILED;
        }
        activeByOwner.remove(ownerId, current);
        return ActivationResult.READY;
    }

    public boolean claim(
            UUID ownerId,
            MountKind kind,
            Entity entity,
            BooleanSupplier deactivate
    ) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(deactivate, "deactivate");
        ActiveMount current = activeByOwner.get(ownerId);
        if (current != null && current.entity().isValid()
                && !current.entity().getUniqueId().equals(entity.getUniqueId())) {
            return false;
        }
        activeByOwner.put(ownerId, new ActiveMount(kind, entity, deactivate));
        return true;
    }

    public void release(UUID ownerId, Entity entity) {
        ActiveMount current = activeByOwner.get(ownerId);
        if (current != null && current.entity().getUniqueId().equals(entity.getUniqueId())) {
            activeByOwner.remove(ownerId, current);
        }
    }

    public MountKind activeKind(UUID ownerId) {
        ActiveMount current = activeByOwner.get(ownerId);
        if (current == null) {
            return null;
        }
        if (!current.entity().isValid()) {
            activeByOwner.remove(ownerId, current);
            return null;
        }
        return current.kind();
    }

    public void clear() {
        activeByOwner.clear();
    }

    public enum ActivationResult {
        READY,
        ALREADY_ACTIVE,
        HAS_PASSENGERS,
        DEACTIVATION_FAILED
    }

    public enum MountKind {
        HORSE,
        DRAGON
    }

    private record ActiveMount(
            MountKind kind,
            Entity entity,
            BooleanSupplier deactivate
    ) {
    }
}
