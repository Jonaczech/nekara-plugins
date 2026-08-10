package cz.nekara.rpg.modules.dragons;

/**
 * Defines the safe airspace needed by the server-controlled dragon carrier.
 *
 * <p>The Ender Dragon is only a visual, so collision is deliberately evaluated
 * against a bounded airspace around the invisible Happy Ghast. Every candidate
 * position must have open sky; this keeps the mount out of caves, tunnels and
 * interiors even though its movement is applied with a teleport.</p>
 */
public final class DragonFlightCollisionPolicy {
    private static final int HORIZONTAL_CLEARANCE = 2;
    private static final int VERTICAL_CLEARANCE = 4;
    private static final double MAXIMUM_SAMPLE_DISTANCE = 0.20;

    private DragonFlightCollisionPolicy() {
    }

    public static boolean canMove(Position origin, Position destination, int maximumAltitude, VoxelAccess voxels) {
        if (origin == null || destination == null || voxels == null || destination.y() > maximumAltitude) {
            return false;
        }
        int samples = Math.max(1, (int) Math.ceil(origin.distanceTo(destination) / MAXIMUM_SAMPLE_DISTANCE));
        for (int sample = 1; sample <= samples; sample++) {
            double progress = (double) sample / samples;
            Position point = origin.interpolate(destination, progress);
            if (!hasClearAirspace(point, voxels) || !hasOpenSky(point, voxels)) {
                return false;
            }
        }
        return true;
    }

    static boolean hasClearAirspace(Position position, VoxelAccess voxels) {
        int centerX = floor(position.x());
        int baseY = floor(position.y());
        int centerZ = floor(position.z());
        for (int x = centerX - HORIZONTAL_CLEARANCE; x <= centerX + HORIZONTAL_CLEARANCE; x++) {
            for (int z = centerZ - HORIZONTAL_CLEARANCE; z <= centerZ + HORIZONTAL_CLEARANCE; z++) {
                for (int y = baseY; y <= baseY + VERTICAL_CLEARANCE; y++) {
                    if (!voxels.isPassable(x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static boolean hasOpenSky(Position position, VoxelAccess voxels) {
        int centerX = floor(position.x());
        int baseY = floor(position.y());
        int centerZ = floor(position.z());
        for (int x = centerX - HORIZONTAL_CLEARANCE; x <= centerX + HORIZONTAL_CLEARANCE; x++) {
            for (int z = centerZ - HORIZONTAL_CLEARANCE; z <= centerZ + HORIZONTAL_CLEARANCE; z++) {
                if (baseY <= voxels.highestBlockingY(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    public record Position(double x, double y, double z) {
        private double distanceTo(Position other) {
            double deltaX = other.x - x;
            double deltaY = other.y - y;
            double deltaZ = other.z - z;
            return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        }

        private Position interpolate(Position other, double progress) {
            return new Position(
                x + (other.x - x) * progress,
                y + (other.y - y) * progress,
                z + (other.z - z) * progress
            );
        }
    }

    public interface VoxelAccess {
        boolean isPassable(int x, int y, int z);

        int highestBlockingY(int x, int z);
    }
}
