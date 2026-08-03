package cz.nekara.rpg.mount;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class MountRecordYamlCodec {
    private static final String ROOT = "mount";

    String encode(MountRecord mount) {
        YamlConfiguration yaml = new YamlConfiguration();
        set(yaml, ROOT, mount);
        return yaml.saveToString();
    }

    MountRecord decode(String value) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(value);
            return get(yaml, ROOT);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid mount payload.", exception);
        }
    }

    private void set(YamlConfiguration yaml, String path, MountRecord mount) {
        yaml.set(path + ".owner-id", mount.ownerId());
        yaml.set(path + ".owner-name", mount.ownerName());
        yaml.set(path + ".last-known-owner-uuid", mount.lastKnownOwnerUuid().toString());
        yaml.set(path + ".mount-id", mount.mountId().toString());
        yaml.set(path + ".active-entity-uuid", mount.activeEntityUuid() == null
                ? null : mount.activeEntityUuid().toString());
        yaml.set(path + ".custom-name", mount.customName());
        yaml.set(path + ".health", mount.health());
        yaml.set(path + ".max-health", mount.maxHealth());
        yaml.set(path + ".movement-speed", mount.movementSpeed());
        yaml.set(path + ".jump-strength", mount.jumpStrength());
        yaml.set(path + ".color", mount.color().name());
        yaml.set(path + ".style", mount.style().name());
        yaml.set(path + ".saddle", mount.saddle());
        yaml.set(path + ".armor", mount.armor());
        yaml.set(path + ".chest", mount.chest());
        for (int slot = 0; slot < mount.storage().size(); slot++) {
            ItemStack item = mount.storage().get(slot);
            if (item != null && !item.isEmpty()) {
                yaml.set(path + ".storage." + slot, item);
            }
        }
        yaml.set(path + ".fire-ticks", mount.fireTicks());
        yaml.set(path + ".freeze-ticks", mount.freezeTicks());
        yaml.set(path + ".remaining-air", mount.remainingAir());
        yaml.set(path + ".potion-effects", mount.potionEffects());
        yaml.set(path + ".summon-available-at", text(mount.summonAvailableAt()));
        yaml.set(path + ".died-at", text(mount.diedAt()));
        yaml.set(path + ".revive-at", text(mount.reviveAt()));
        yaml.set(path + ".updated-at", mount.updatedAt().toString());
    }

    private MountRecord get(YamlConfiguration yaml, String path) {
        String active = yaml.getString(path + ".active-entity-uuid");
        String summon = yaml.getString(path + ".summon-available-at");
        String died = yaml.getString(path + ".died-at");
        String revive = yaml.getString(path + ".revive-at");
        return new MountRecord(
                required(yaml.getString(path + ".owner-id")),
                required(yaml.getString(path + ".owner-name")),
                UUID.fromString(required(yaml.getString(path + ".last-known-owner-uuid"))),
                UUID.fromString(required(yaml.getString(path + ".mount-id"))),
                active == null ? null : UUID.fromString(active),
                required(yaml.getString(path + ".custom-name")),
                yaml.getDouble(path + ".health"), yaml.getDouble(path + ".max-health"),
                yaml.getDouble(path + ".movement-speed"), yaml.getDouble(path + ".jump-strength"),
                Horse.Color.valueOf(required(yaml.getString(path + ".color"))),
                Horse.Style.valueOf(required(yaml.getString(path + ".style"))),
                yaml.getItemStack(path + ".saddle"), yaml.getItemStack(path + ".armor"),
                yaml.getItemStack(path + ".chest"), storage(yaml, path + ".storage"),
                yaml.getInt(path + ".fire-ticks"), yaml.getInt(path + ".freeze-ticks"),
                yaml.getInt(path + ".remaining-air"), effects(yaml.getList(path + ".potion-effects", List.of())),
                instant(summon), instant(died), instant(revive),
                Instant.parse(required(yaml.getString(path + ".updated-at")))
        );
    }

    private List<ItemStack> storage(YamlConfiguration yaml, String path) {
        List<ItemStack> result = new ArrayList<>(Collections.nCopies(MountRecord.STORAGE_SIZE, null));
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            int slot = Integer.parseInt(key);
            if (slot < 0 || slot >= MountRecord.STORAGE_SIZE) {
                throw new IllegalArgumentException("Invalid storage slot.");
            }
            ItemStack item = yaml.getItemStack(path + "." + key);
            if (item == null || item.isEmpty()) throw new IllegalArgumentException("Empty storage item.");
            result.set(slot, item);
        }
        return result;
    }

    private List<PotionEffect> effects(List<?> values) {
        List<PotionEffect> result = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof PotionEffect effect)) throw new IllegalArgumentException("Invalid potion effect.");
            result.add(effect);
        }
        return List.copyOf(result);
    }

    private String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing mount value.");
        return value;
    }

    private String text(Instant value) {
        return value == null ? null : value.toString();
    }

    private Instant instant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
