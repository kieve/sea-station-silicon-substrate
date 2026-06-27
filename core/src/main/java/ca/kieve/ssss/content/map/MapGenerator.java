package ca.kieve.ssss.content.map;

import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.MapContext;
import ca.kieve.ssss.context.WorldContext;

import java.util.List;

public interface MapGenerator {
    void generate(
        BlockTypeFactory blockTypeFactory,
        MapContext mapContext,
        WorldContext worldContext,
        FluidContext fluidContext
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
