package ca.kieve.ssss.content.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.content.ContentLoader;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.context.MapContext;
import ca.kieve.ssss.context.WorldContext;
import ca.kieve.ssss.testharness.HeadlessGdxBootstrap;
import ca.kieve.ssss.util.Vec3i;
import ca.kieve.ssss.world.MapRegion;
import ca.kieve.ssss.world.WorldModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads a real test-resource map (test/empty.yaml) and asserts the
 * generator produces the expected WorldModel and registers the expected
 * region into the supplied MapContext.
 */
class YamlMapGeneratorTest {
    private ContentRegistry content;
    private MapContext mapContext;
    private WorldContext worldContext;

    @BeforeEach
    void setUp() {
        HeadlessGdxBootstrap.ensureInitialized();
        content = new ContentLoader().loadAll();
        mapContext = new MapContext();
        worldContext = new WorldContext();
    }

    @Test
    void generateProducesWorldWithExpectedDimensions() {
        YamlMapGenerator generator = new YamlMapGenerator("test/empty.yaml");

        generator.generate(content.getBlockTypeFactory(), mapContext, worldContext);
        WorldModel world = worldContext.getModel();

        // test/empty.yaml is a 9×9 grid with 3 z-layers
        assertEquals(9, world.getWidth());
        assertEquals(9, world.getHeight());
        assertEquals(3, world.getDepth());
    }

    @Test
    void generateRegistersSingleRegionWithExpectedFields() {
        YamlMapGenerator generator = new YamlMapGenerator("test/empty.yaml");

        generator.generate(content.getBlockTypeFactory(), mapContext, worldContext);
        var regions = mapContext.getRegions();

        assertEquals(1, regions.size());
        MapRegion region = regions.get(0);
        assertEquals("test/empty", region.id());
        assertEquals(new Vec3i(0, 0, 0), region.box().origin());
        assertEquals(new Vec3i(9, 9, 3), region.box().size());
    }

    @Test
    void regionContainsAllWorldCells() {
        YamlMapGenerator generator = new YamlMapGenerator("test/empty.yaml");

        generator.generate(content.getBlockTypeFactory(), mapContext, worldContext);
        MapRegion region = mapContext.getRegions().get(0);

        assertTrue(region.contains(new Vec3i(0, 0, 0)));
        assertTrue(region.contains(new Vec3i(8, 8, 2)));
        assertFalse(region.contains(new Vec3i(9, 0, 0)));
    }

    @Test
    void getFloorGlyphIdPreservedFromYaml() {
        YamlMapGenerator generator = new YamlMapGenerator("test/empty.yaml");

        generator.generate(content.getBlockTypeFactory(), mapContext, worldContext);

        assertEquals("interpunct", generator.getFloorGlyphId());
    }

    @Test
    void getEntitiesPreservedFromYaml() {
        YamlMapGenerator generator = new YamlMapGenerator("test/empty.yaml");

        generator.generate(content.getBlockTypeFactory(), mapContext, worldContext);
        var entities = generator.getEntities();

        assertNotNull(entities);
        assertEquals(1, entities.size());
        assertEquals("player", entities.get(0).id());
    }
}
