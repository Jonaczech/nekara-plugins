package cz.nekara.rpg.skills.experience;

import cz.nekara.rpg.skills.SkillId;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;

/** One persisted, time-bounded server event. Expiry is checked from UTC epoch time. */
public final class GlobalExperienceEvent {
    private final File file;
    private SkillId skill;
    private double multiplier = 1.0;
    private long endsAtEpochMillis;
    public GlobalExperienceEvent(File file) { this.file = Objects.requireNonNull(file); load(); }
    public synchronized void start(SkillId skill, double multiplier, long endsAtEpochMillis) {
        if (!Double.isFinite(multiplier) || multiplier < 1.0 || multiplier > 5.0 || endsAtEpochMillis <= System.currentTimeMillis()) {
            throw new IllegalArgumentException("Násobitel musí být 1 až 5 a konec události musí být v budoucnu.");
        }
        this.skill = skill; this.multiplier = multiplier; this.endsAtEpochMillis = endsAtEpochMillis; save();
    }
    public synchronized void stop() { skill = null; multiplier = 1.0; endsAtEpochMillis = 0; save(); }
    public synchronized double multiplier(SkillId target) {
        return active() && (skill == null || skill == target) ? multiplier : 1.0;
    }
    public synchronized boolean active() { return endsAtEpochMillis > System.currentTimeMillis(); }
    public synchronized SkillId skill() { return active() ? skill : null; }
    public synchronized double configuredMultiplier() { return active() ? multiplier : 1.0; }
    public synchronized long endsAtEpochMillis() { return active() ? endsAtEpochMillis : 0; }
    private void load() {
        if (!file.isFile()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = yaml.getString("skill", "all");
        skill = "all".equalsIgnoreCase(id) ? null : java.util.Arrays.stream(SkillId.values()).filter(value -> value.id().equalsIgnoreCase(id)).findFirst().orElse(null);
        multiplier = yaml.getDouble("multiplier", 1.0); endsAtEpochMillis = yaml.getLong("ends-at-epoch-millis", 0);
    }
    private void save() {
        try {
            File parent = file.getParentFile(); if (parent != null) parent.mkdirs();
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("skill", skill == null ? "all" : skill.id()); yaml.set("multiplier", multiplier); yaml.set("ends-at-epoch-millis", endsAtEpochMillis); yaml.save(file);
        } catch (IOException exception) { throw new IllegalStateException("Nelze uložit XP událost", exception); }
    }
}
