package cz.nekara.rpg.updater;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SemanticVersion(int major, int minor, int patch) implements Comparable<SemanticVersion> {
    private static final Pattern STABLE_VERSION = Pattern.compile(
            "^v?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$");

    public SemanticVersion {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components must not be negative");
        }
    }

    public static SemanticVersion parseStable(String value) {
        Objects.requireNonNull(value, "value");
        Matcher matcher = STABLE_VERSION.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Expected a stable semantic version, got: " + value);
        }
        try {
            return new SemanticVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            );
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Semantic version component is too large: " + value, exception);
        }
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int majorComparison = Integer.compare(major, other.major);
        if (majorComparison != 0) {
            return majorComparison;
        }
        int minorComparison = Integer.compare(minor, other.minor);
        return minorComparison != 0 ? minorComparison : Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
