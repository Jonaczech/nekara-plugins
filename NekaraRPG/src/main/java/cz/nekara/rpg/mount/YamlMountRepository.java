package cz.nekara.rpg.mount;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class YamlMountRepository implements MountRepository {
    private static final int SCHEMA_VERSION = 1;
    static final String LEGACY_MOUNT_NAME = "Bezejmenný";

    private final File file;
    private final Map<String, MountRecord> mountsByOwner = new HashMap<>();
    private final Map<UUID, String> ownersByMount = new HashMap<>();
    private final Map<String, Instant> combatUntil = new HashMap<>();

    public YamlMountRepository(File file) throws IOException {
        this.file = file;
        load();
    }

    @Override
    public synchronized Optional<MountRecord> findByOwnerId(String ownerId) {
        return Optional.ofNullable(mountsByOwner.get(ownerId));
    }

    @Override
    public synchronized Optional<MountRecord> findByMountId(UUID mountId) {
        String ownerId = ownersByMount.get(mountId);
        return ownerId == null ? Optional.empty() : Optional.ofNullable(mountsByOwner.get(ownerId));
    }

    @Override
    public synchronized Collection<MountRecord> findAll() {
        return List.copyOf(mountsByOwner.values());
    }

    @Override
    public synchronized boolean create(MountRecord mount) throws IOException {
        if (mountsByOwner.containsKey(mount.ownerId()) || ownersByMount.containsKey(mount.mountId())) {
            return false;
        }
        mountsByOwner.put(mount.ownerId(), mount);
        ownersByMount.put(mount.mountId(), mount.ownerId());
        try {
            save();
            return true;
        } catch (IOException | RuntimeException exception) {
            mountsByOwner.remove(mount.ownerId());
            ownersByMount.remove(mount.mountId());
            throw storageException("Could not create mount record.", exception);
        }
    }

    @Override
    public synchronized void update(MountRecord mount) throws IOException {
        MountRecord previous = mountsByOwner.get(mount.ownerId());
        if (previous == null || !previous.mountId().equals(mount.mountId())) {
            throw new IOException("Mount ownership does not match the persisted record.");
        }
        mountsByOwner.put(mount.ownerId(), mount);
        try {
            save();
        } catch (IOException | RuntimeException exception) {
            mountsByOwner.put(previous.ownerId(), previous);
            throw storageException("Could not update mount record.", exception);
        }
    }

    @Override
    public synchronized Optional<Instant> combatUntil(String ownerId) {
        return Optional.ofNullable(combatUntil.get(ownerId));
    }

    @Override
    public synchronized void setCombatUntil(Map<String, Instant> combatWindows) throws IOException {
        Map<String, Instant> previous = new HashMap<>();
        for (Map.Entry<String, Instant> entry : combatWindows.entrySet()) {
            previous.put(entry.getKey(), combatUntil.put(entry.getKey(), entry.getValue()));
        }
        try {
            save();
        } catch (IOException | RuntimeException exception) {
            for (Map.Entry<String, Instant> entry : previous.entrySet()) {
                if (entry.getValue() == null) {
                    combatUntil.remove(entry.getKey());
                } else {
                    combatUntil.put(entry.getKey(), entry.getValue());
                }
            }
            throw storageException("Could not update combat window.", exception);
        }
    }

    private void load() throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        if (!file.exists()) {
            save();
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (InvalidConfigurationException exception) {
            throw new IOException("Invalid NekaraMounts storage.", exception);
        }
        if (yaml.getInt("schema-version", -1) != SCHEMA_VERSION) {
            throw new IOException("Unsupported or missing NekaraMounts storage schema version.");
        }

        boolean migratedLegacyName = false;
        ConfigurationSection mounts = yaml.getConfigurationSection("mounts");
        if (mounts != null) {
            for (String ownerId : mounts.getKeys(false)) {
                migratedLegacyName |= loadMount(yaml, ownerId);
            }
        }
        ConfigurationSection combat = yaml.getConfigurationSection("combat");
        if (combat != null) {
            for (String ownerId : combat.getKeys(false)) {
                try {
                    combatUntil.put(ownerId, Instant.parse(required(
                            yaml.getString("combat." + ownerId), "combat timestamp")));
                } catch (IllegalArgumentException | DateTimeParseException exception) {
                    throw new IOException("Invalid combat timestamp for '" + ownerId + "'.", exception);
                }
            }
        }
        if (migratedLegacyName) {
            save();
        }
    }

    private boolean loadMount(YamlConfiguration yaml, String ownerId) throws IOException {
        String path = "mounts." + ownerId;
        try {
            UUID mountId = UUID.fromString(required(yaml.getString(path + ".mount-id"), "mount-id"));
            String configuredOwnerId = required(yaml.getString(path + ".owner-id"), "owner-id");
            if (!ownerId.equals(configuredOwnerId)) {
                throw new IllegalArgumentException("Mount key and owner-id do not match.");
            }
            String activeEntityValue = yaml.getString(path + ".active-entity-uuid");
            String diedAtValue = yaml.getString(path + ".died-at");
            String reviveAtValue = yaml.getString(path + ".revive-at");
            String summonAvailableAtValue = yaml.getString(path + ".summon-available-at");
            String storedCustomName = yaml.getString(path + ".custom-name");
            boolean missingCustomName = storedCustomName == null || storedCustomName.isBlank();
            MountRecord mount = new MountRecord(
                    ownerId,
                    required(yaml.getString(path + ".owner-name"), "owner-name"),
                    UUID.fromString(required(yaml.getString(path + ".last-known-owner-uuid"),
                            "last-known-owner-uuid")),
                    mountId,
                    activeEntityValue == null ? null : UUID.fromString(activeEntityValue),
                    missingCustomName ? LEGACY_MOUNT_NAME : storedCustomName,
                    yaml.getDouble(path + ".health"),
                    yaml.getDouble(path + ".max-health"),
                    yaml.getDouble(path + ".movement-speed"),
                    yaml.getDouble(path + ".jump-strength"),
                    Horse.Color.valueOf(required(yaml.getString(path + ".color"), "color")),
                    Horse.Style.valueOf(required(yaml.getString(path + ".style"), "style")),
                    yaml.getItemStack(path + ".saddle"),
                    yaml.getItemStack(path + ".armor"),
                    yaml.getInt(path + ".fire-ticks"),
                    yaml.getInt(path + ".freeze-ticks"),
                    yaml.getInt(path + ".remaining-air"),
                    readPotionEffects(yaml.getList(path + ".potion-effects", List.of())),
                    summonAvailableAtValue == null ? null : Instant.parse(summonAvailableAtValue),
                    diedAtValue == null ? null : Instant.parse(diedAtValue),
                    reviveAtValue == null ? null : Instant.parse(reviveAtValue),
                    Instant.parse(required(yaml.getString(path + ".updated-at"), "updated-at"))
            );
            if (mountsByOwner.put(ownerId, mount) != null
                    || ownersByMount.put(mountId, ownerId) != null) {
                throw new IllegalArgumentException("Duplicate owner or mount id.");
            }
            return missingCustomName;
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new IOException("Invalid NekaraMounts record '" + ownerId + "'.", exception);
        }
    }

    private List<PotionEffect> readPotionEffects(List<?> values) {
        List<PotionEffect> effects = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof PotionEffect effect)) {
                throw new IllegalArgumentException("Potion effect is not serializable.");
            }
            effects.add(effect);
        }
        return List.copyOf(effects);
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + field + ".");
        }
        return value;
    }

    private IOException storageException(String message, Exception cause) {
        return cause instanceof IOException ioException
                ? ioException : new IOException(message, cause);
    }

    private void save() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("schema-version", SCHEMA_VERSION);
        for (MountRecord mount : mountsByOwner.values()) {
            String path = "mounts." + mount.ownerId();
            yaml.set(path + ".owner-id", mount.ownerId());
            yaml.set(path + ".owner-name", mount.ownerName());
            yaml.set(path + ".last-known-owner-uuid", mount.lastKnownOwnerUuid().toString());
            yaml.set(path + ".mount-id", mount.mountId().toString());
            yaml.set(path + ".active-entity-uuid",
                    mount.activeEntityUuid() == null ? null : mount.activeEntityUuid().toString());
            yaml.set(path + ".custom-name", mount.customName());
            yaml.set(path + ".health", mount.health());
            yaml.set(path + ".max-health", mount.maxHealth());
            yaml.set(path + ".movement-speed", mount.movementSpeed());
            yaml.set(path + ".jump-strength", mount.jumpStrength());
            yaml.set(path + ".color", mount.color().name());
            yaml.set(path + ".style", mount.style().name());
            yaml.set(path + ".saddle", mount.saddle());
            yaml.set(path + ".armor", mount.armor());
            yaml.set(path + ".fire-ticks", mount.fireTicks());
            yaml.set(path + ".freeze-ticks", mount.freezeTicks());
            yaml.set(path + ".remaining-air", mount.remainingAir());
            yaml.set(path + ".potion-effects", mount.potionEffects());
            yaml.set(path + ".summon-available-at", mount.summonAvailableAt() == null
                    ? null : mount.summonAvailableAt().toString());
            yaml.set(path + ".died-at", mount.diedAt() == null ? null : mount.diedAt().toString());
            yaml.set(path + ".revive-at", mount.reviveAt() == null ? null : mount.reviveAt().toString());
            yaml.set(path + ".updated-at", mount.updatedAt().toString());
        }
        for (Map.Entry<String, Instant> entry : combatUntil.entrySet()) {
            yaml.set("combat." + entry.getKey(), entry.getValue().toString());
        }

        File parent = file.getParentFile();
        File temporary = new File(parent == null ? new File(".") : parent, file.getName() + ".tmp");
        yaml.save(temporary);
        try {
            Files.move(temporary.toPath(), file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
