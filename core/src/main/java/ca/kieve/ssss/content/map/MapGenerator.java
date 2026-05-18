package ca.kieve.ssss.content.map;

import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.context.MapContext;
import ca.kieve.ssss.context.WorldContext;

import java.util.List;

/**
 * Interface for map generators that produce 3D voxel worlds.
 * Implementations can generate static test maps, procedural dungeons,
 * or load maps from files.
 */
public interface MapGenerator {
    /**
     * Generates the world geometry: populates {@code worldContext} with
     * a fresh {@link ca.kieve.ssss.world.WorldModel} and registers every
     * produced {@link ca.kieve.ssss.world.MapRegion} into
     * {@code mapContext}.
     */
    void generate(
        BlockTypeFactory blockTypeFactory,
        MapContext mapContext,
        WorldContext worldContext
    );

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
