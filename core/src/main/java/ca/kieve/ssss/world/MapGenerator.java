package ca.kieve.ssss.world;

import ca.kieve.ssss.content.BlockTypeFactory;
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
     * @param blockTypeFactory Factory for querying block type properties
     * @return The generated world model
     */
    WorldModel generate(BlockTypeFactory blockTypeFactory);

    /**
     * Returns a suggested starting position for the player.
     * This should be a position where the player can stand
     * (air block with solid block below).
     *
     * @return The suggested player spawn position
     */
    Vec3i getPlayerSpawn();
}
