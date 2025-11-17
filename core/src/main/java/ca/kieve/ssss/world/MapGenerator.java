package ca.kieve.ssss.world;

import ca.kieve.ssss.util.Vec3i;

/**
 * Interface for map generators that produce 3D voxel worlds.
 * Implementations can generate static test maps, procedural dungeons,
 * or load maps from files.
 */
public interface MapGenerator {

    /**
     * Generates a WorldModel containing the 3D block data.
     *
     * @return The generated world model
     */
    WorldModel generate();

    /**
     * Returns a suggested starting position for the player.
     * This should be a position where the player can stand
     * (air block with solid block below).
     *
     * @return The suggested player spawn position
     */
    Vec3i getPlayerSpawn();
}
