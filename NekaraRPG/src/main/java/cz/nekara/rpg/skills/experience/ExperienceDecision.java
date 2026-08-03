package cz.nekara.rpg.skills.experience;

import java.util.Objects;

public record ExperienceDecision(boolean allowed, double multiplier, ExperienceReason reason) {
    public ExperienceDecision {
        Objects.requireNonNull(reason, "reason");
        if (!Double.isFinite(multiplier) || multiplier < 0 || multiplier > 1) {
            throw new IllegalArgumentException("Experience multiplier must be between 0 and 1");
        }
        if (allowed && multiplier <= 0) {
            throw new IllegalArgumentException("Allowed experience must have a positive multiplier");
        }
        if (!allowed && multiplier != 0) {
            throw new IllegalArgumentException("Denied experience must have a zero multiplier");
        }
    }

    public static ExperienceDecision allow(double multiplier, ExperienceReason reason) {
        return new ExperienceDecision(true, multiplier, reason);
    }

    public static ExperienceDecision deny(ExperienceReason reason) {
        return new ExperienceDecision(false, 0, reason);
    }
}
