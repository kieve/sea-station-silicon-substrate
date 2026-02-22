package ca.kieve.ssss.world;

import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.content.MapEntityDefinition;

import java.util.List;

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
     * Returns the glyph ID to use for rendering floor tiles.
     *
     * @return The glyph identifier for floor rendering
     */
    String getFloorGlyphId();

    /**
     * Returns the entity definitions for this map.
     *
     * @return List of map entity definitions to spawn
     */
    List<MapEntityDefinition> getEntities();
}
