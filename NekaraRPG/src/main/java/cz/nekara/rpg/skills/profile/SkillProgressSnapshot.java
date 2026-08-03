package cz.nekara.rpg.skills.profile;

import cz.nekara.rpg.skills.PowerProgress;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillLevelProgress;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SkillProgressSnapshot {
    private final Map<SkillId, SkillLevelProgress> skills;
    private final PowerProgress power;

    public SkillProgressSnapshot(Map<SkillId, SkillLevelProgress> skills, PowerProgress power) {
        Objects.requireNonNull(skills, "skills");
        this.power = Objects.requireNonNull(power, "power");
        EnumMap<SkillId, SkillLevelProgress> normalized = new EnumMap<>(SkillId.class);
        for (SkillId skill : SkillId.gameplaySkills()) {
            SkillLevelProgress progress = skills.get(skill);
            if (progress == null) {
                throw new IllegalArgumentException("Missing progress for " + skill.id());
            }
            normalized.put(skill, progress);
        }
        if (skills.containsKey(SkillId.POWER)) {
            throw new IllegalArgumentException("Power is stored separately as derived progress");
        }
        this.skills = Map.copyOf(normalized);
    }

    public SkillLevelProgress skill(SkillId skill) {
        SkillLevelProgress progress = skills.get(Objects.requireNonNull(skill, "skill"));
        if (progress == null) {
            throw new IllegalArgumentException("Power is not direct skill progress");
        }
        return progress;
    }

    public Map<SkillId, SkillLevelProgress> skills() {
        return skills;
    }

    public PowerProgress power() {
        return power;
    }
}
