package cz.nekara.rpg.modules.dragons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonFlightCollisionPolicyTest {
    @Test
    void allowsFlightThroughClearOpenSky() {
        assertTrue(DragonFlightCollisionPolicy.canMove(
            new DragonFlightCollisionPolicy.Position(0.0, 80.0, 0.0),
            new DragonFlightCollisionPolicy.Position(1.0, 80.0, 0.0),
            256, clearSky()));
    }

    @Test
    void rejectsABlockingVoxelAnywhereAlongTheRoute() {
        DragonFlightCollisionPolicy.VoxelAccess voxels = new DragonFlightCollisionPolicy.VoxelAccess() {
            @Override
            public boolean isPassable(int x, int y, int z) {
                return !(x == 3 && y == 80 && z == 0);
            }

            @Override
            public int highestBlockingY(int x, int z) {
                return 64;
            }
        };

        assertFalse(DragonFlightCollisionPolicy.canMove(
            new DragonFlightCollisionPolicy.Position(0.0, 80.0, 0.0),
            new DragonFlightCollisionPolicy.Position(6.0, 80.0, 0.0),
            256, voxels));
    }

    @Test
    void rejectsFlightBelowTheSurfaceEvenWhenTheImmediateAirspaceIsClear() {
        DragonFlightCollisionPolicy.VoxelAccess cave = new DragonFlightCollisionPolicy.VoxelAccess() {
            @Override
            public boolean isPassable(int x, int y, int z) {
                return true;
            }

            @Override
            public int highestBlockingY(int x, int z) {
                return 70;
            }
        };

        assertFalse(DragonFlightCollisionPolicy.canMove(
            new DragonFlightCollisionPolicy.Position(0.0, 60.0, 0.0),
            new DragonFlightCollisionPolicy.Position(0.0, 60.2, 0.0),
            256, cave));
    }

    @Test
    void rejectsDestinationsAboveTheConfiguredAltitude() {
        assertFalse(DragonFlightCollisionPolicy.canMove(
            new DragonFlightCollisionPolicy.Position(0.0, 255.0, 0.0),
            new DragonFlightCollisionPolicy.Position(0.0, 257.0, 0.0),
            256, clearSky()));
    }

    private static DragonFlightCollisionPolicy.VoxelAccess clearSky() {
        return new DragonFlightCollisionPolicy.VoxelAccess() {
            @Override
            public boolean isPassable(int x, int y, int z) {
                return true;
            }

            @Override
            public int highestBlockingY(int x, int z) {
                return 64;
            }
        };
    }
}
