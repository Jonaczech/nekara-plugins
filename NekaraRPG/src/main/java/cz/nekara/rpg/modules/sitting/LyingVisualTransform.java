package cz.nekara.rpg.modules.sitting;

import org.bukkit.Location;

final class LyingVisualTransform {
    private final double yawOffsetDegrees;
    private final double forwardOffset;
    private final double sideOffset;
    private final double verticalOffset;

    LyingVisualTransform(
        double yawOffsetDegrees,
        double forwardOffset,
        double sideOffset,
        double verticalOffset
    ) {
        this.yawOffsetDegrees = yawOffsetDegrees;
        this.forwardOffset = forwardOffset;
        this.sideOffset = sideOffset;
        this.verticalOffset = verticalOffset;
    }

    Location apply(Location subjectLocation) {
        Location transformed = subjectLocation.clone();
        double yawRadians = Math.toRadians(subjectLocation.getYaw());
        double forwardX = -Math.sin(yawRadians);
        double forwardZ = Math.cos(yawRadians);
        double rightX = -Math.cos(yawRadians);
        double rightZ = -Math.sin(yawRadians);
        transformed.add(
            forwardX * forwardOffset + rightX * sideOffset,
            verticalOffset,
            forwardZ * forwardOffset + rightZ * sideOffset
        );
        transformed.setYaw(normalizeYaw((float) (subjectLocation.getYaw() + yawOffsetDegrees)));
        transformed.setPitch(0.0F);
        return transformed;
    }

    static float normalizeYaw(float yaw) {
        float normalized = yaw % 360.0F;
        if (normalized >= 180.0F) {
            normalized -= 360.0F;
        } else if (normalized < -180.0F) {
            normalized += 360.0F;
        }
        return normalized;
    }
}
