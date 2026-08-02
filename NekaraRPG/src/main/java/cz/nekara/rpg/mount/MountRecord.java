package cz.nekara.rpg.mount;

import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record MountRecord(
        String ownerId,
        String ownerName,
        UUID lastKnownOwnerUuid,
        UUID mountId,
        UUID activeEntityUuid,
        String customName,
        double health,
        double maxHealth,
        double movementSpeed,
        double jumpStrength,
        Horse.Color color,
        Horse.Style style,
        ItemStack saddle,
        ItemStack armor,
        int fireTicks,
        int freezeTicks,
        int remainingAir,
        List<PotionEffect> potionEffects,
        Instant summonAvailableAt,
        Instant diedAt,
        Instant reviveAt,
        Instant updatedAt
) {
    public MountRecord {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(ownerName, "ownerName");
        Objects.requireNonNull(lastKnownOwnerUuid, "lastKnownOwnerUuid");
        Objects.requireNonNull(mountId, "mountId");
        Objects.requireNonNull(customName, "customName");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (customName.isBlank()) {
            throw new IllegalArgumentException("Mount name must not be blank.");
        }
        if (health < 0.0 || maxHealth <= 0.0 || health > maxHealth
                || maxHealth > 2_048.0 || movementSpeed <= 0.0 || movementSpeed > 10.0
                || jumpStrength < 0.0 || jumpStrength > 2.0) {
            throw new IllegalArgumentException("Mount attributes are outside the supported range.");
        }
        if ((diedAt == null) != (reviveAt == null)) {
            throw new IllegalArgumentException("Death and revival timestamps must be both present or both absent.");
        }
        saddle = cloneItem(saddle);
        armor = cloneItem(armor);
        potionEffects = List.copyOf(potionEffects == null ? List.of() : potionEffects);
    }

    @Override
    public ItemStack saddle() {
        return cloneItem(saddle);
    }

    @Override
    public ItemStack armor() {
        return cloneItem(armor);
    }

    public boolean isDead() {
        return diedAt != null;
    }

    public MountRecord withRuntimeState(
            UUID entityUuid,
            String name,
            double currentHealth,
            double currentMaxHealth,
            double currentMovementSpeed,
            double currentJumpStrength,
            Horse.Color currentColor,
            Horse.Style currentStyle,
            ItemStack currentSaddle,
            ItemStack currentArmor,
            int currentFireTicks,
            int currentFreezeTicks,
            int currentRemainingAir,
            List<PotionEffect> currentPotionEffects,
            Instant now
    ) {
        return new MountRecord(ownerId, ownerName, lastKnownOwnerUuid, mountId, entityUuid,
                name, currentHealth, currentMaxHealth, currentMovementSpeed, currentJumpStrength,
                currentColor, currentStyle, currentSaddle, currentArmor, currentFireTicks,
                currentFreezeTicks, currentRemainingAir, currentPotionEffects,
                summonAvailableAt, null, null, now);
    }

    public MountRecord dormant(Instant now) {
        return new MountRecord(ownerId, ownerName, lastKnownOwnerUuid, mountId, null,
                customName, health, maxHealth, movementSpeed, jumpStrength, color, style,
                saddle, armor, fireTicks, freezeTicks, remainingAir, potionEffects,
                summonAvailableAt, diedAt, reviveAt, now);
    }

    public MountRecord killed(Instant deathTime, Instant revivalTime) {
        return new MountRecord(ownerId, ownerName, lastKnownOwnerUuid, mountId, null,
                customName, 0.0, maxHealth, movementSpeed, jumpStrength, color, style,
                saddle, armor, fireTicks, freezeTicks, remainingAir, potionEffects,
                summonAvailableAt, deathTime, revivalTime, deathTime);
    }

    public MountRecord revived(Instant now) {
        return new MountRecord(ownerId, ownerName, lastKnownOwnerUuid, mountId, null,
                customName, maxHealth, maxHealth, movementSpeed, jumpStrength, color, style,
                saddle, armor, fireTicks, freezeTicks, remainingAir, potionEffects,
                summonAvailableAt, null, null, now);
    }

    public MountRecord withIdentity(String name, Horse.Color newColor, Instant now) {
        return new MountRecord(ownerId, ownerName, lastKnownOwnerUuid, mountId, activeEntityUuid,
                name, health, maxHealth, movementSpeed, jumpStrength, newColor, style,
                saddle, armor, fireTicks, freezeTicks, remainingAir, potionEffects,
                summonAvailableAt, diedAt, reviveAt, now);
    }

    public MountRecord withEquipment(ItemStack newSaddle, ItemStack newArmor, Instant now) {
        return new MountRecord(ownerId, ownerName, lastKnownOwnerUuid, mountId, activeEntityUuid,
                customName, health, maxHealth, movementSpeed, jumpStrength, color, style,
                newSaddle, newArmor, fireTicks, freezeTicks, remainingAir, potionEffects,
                summonAvailableAt, diedAt, reviveAt, now);
    }

    public MountRecord withSummonAvailableAt(Instant availableAt, Instant now) {
        return new MountRecord(ownerId, ownerName, lastKnownOwnerUuid, mountId, activeEntityUuid,
                customName, health, maxHealth, movementSpeed, jumpStrength, color, style,
                saddle, armor, fireTicks, freezeTicks, remainingAir, potionEffects,
                availableAt, diedAt, reviveAt, now);
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null || item.isEmpty() ? null : item.clone();
    }
}
