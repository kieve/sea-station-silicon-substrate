package ca.kieve.ssss.content.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentLoader;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.MapContext;
import ca.kieve.ssss.context.WorldContext;
import ca.kieve.ssss.testharness.HeadlessGdxBootstrap;
import ca.kieve.ssss.util.Vec3i;
import ca.kieve.ssss.world.WorldModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the composite paths of {@link YamlMapGenerator}: world sizing,
 * region registration, cell stamping with overlap detection, entity
 * translation.
 */
class YamlMapGeneratorCompositeTest {
    private ContentRegistry content;
    private MapContext mapContext;
    private WorldContext worldContext;
    private FluidContext fluidContext;

    @BeforeEach
    void setUp() {
        HeadlessGdxBootstrap.ensureInitialized();
        content = new ContentLoader().loadAll();
        mapContext = new MapContext();
        worldContext = new WorldContext();
        fluidContext = new FluidContext();
    }

    @Test
    void twoRoomsOffsetSizesWorldToTheUnion() {
        YamlMapGenerator gen = new YamlMapGenerator("test/composite/two_rooms_offset.yaml");

        gen.generate(content.getBlockTypeFactory(), mapContext, worldContext, fluidContext);
        WorldModel world = worldContext.getModel();

        // leaf_a (3x3x1) at (0,0,0) ∪ leaf_b (3x3x1) at (3,0,0) → 6x3x1
        assertEquals(6, world.getWidth());
        assertEquals(3, world.getHeight());
        assertEquals(1, world.getDepth());
    }

    @Test
    void twoRoomsOffsetRegistersOneRegionPerLeaf() {
        YamlMapGenerator gen = new YamlMapGenerator("test/composite/two_rooms_offset.yaml");

        gen.generate(content.getBlockTypeFactory(), mapContext, worldContext, fluidContext);

        // pure-composition root contributes no region; only the two leafs
        assertEquals(2, mapContext.getRegions().size());
        var ids = mapContext.getRegions().stream().map(r -> r.id()).toList();
        assertTrue(ids.contains("a"), ids.toString());
        assertTrue(ids.contains("b"), ids.toString());
    }

    @Test
    void regionAtConsultsCellStampsAfterComposite() {
        YamlMapGenerator gen = new YamlMapGenerator("test/composite/two_rooms_offset.yaml");

        gen.generate(content.getBlockTypeFactory(), mapContext, worldContext, fluidContext);

        var leafA = mapContext.regionAt(new Vec3i(0, 0, 0));
        var leafB = mapContext.regionAt(new Vec3i(3, 0, 0));
        var leafBFar = mapContext.regionAt(new Vec3i(5, 2, 0));
        assertNotNull(leafA);
        assertNotNull(leafB);
        assertEquals("a", leafA.id());
        assertEquals("b", leafB.id());
        assertSame(leafB, leafBFar);
    }

    @Test
    void connectorModeYieldsExpectedWorld() {
        YamlMapGenerator gen = new YamlMapGenerator("test/composite/two_rooms_connector.yaml");

        gen.generate(content.getBlockTypeFactory(), mapContext, worldContext, fluidContext);
        WorldModel world = worldContext.getModel();

        // leaf_a at (0,0,0) ∪ leaf_b at (6,0,0) → 9x3x1
        assertEquals(9, world.getWidth());
        assertEquals(3, world.getHeight());
        assertEquals(1, world.getDepth());
        // gap between (3..6) is untouched air
        var unowned = mapContext.regionAt(new Vec3i(4, 1, 0));
        assertEquals(null, unowned);
    }

    @Test
    void nestedThreeDeepPlacesLeafAtComposedOffset() {
        YamlMapGenerator gen = new YamlMapGenerator("test/composite/nested_three_deep.yaml");

        gen.generate(content.getBlockTypeFactory(), mapContext, worldContext, fluidContext);

        var leaf = mapContext.regionAt(new Vec3i(1, 1, 0));
        assertNotNull(leaf);
        // nested_mid.yaml's submap entity is id "leaf"
        assertEquals("leaf", leaf.id());
    }

    @Test
    void overlapConflictWithoutAllowOverlapThrows() {
        YamlMapGenerator gen = new YamlMapGenerator("test/composite/overlap_conflict.yaml");

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> gen
                .generate(content.getBlockTypeFactory(), mapContext, worldContext, fluidContext)
        );
        assertTrue(
            ex.getMessage().toLowerCase().contains("overlap"),
            "expected 'overlap' in message, was: " + ex.getMessage()
        );
    }

    @Test
    void overlapAllowedYieldsLastWriterWins() {
        YamlMapGenerator gen = new YamlMapGenerator("test/composite/overlap_allowed.yaml");

        gen.generate(content.getBlockTypeFactory(), mapContext, worldContext, fluidContext);

        // Cells (1..2, *, 0) are claimed by both; "second" (placed last)
        // owns them after last-writer-wins.
        var owner = mapContext.regionAt(new Vec3i(1, 0, 0));
        assertNotNull(owner);
        assertEquals("second", owner.id());
        // Cells (0, *, 0) are only in "first"
        var firstOnly = mapContext.regionAt(new Vec3i(0, 0, 0));
        assertNotNull(firstOnly);
        assertEquals("first", firstOnly.id());
    }

    @Test
    void childEntityPositionsAreTranslatedByWorldOffset() {
        YamlMapGenerator gen = new YamlMapGenerator(
            "test/composite/composition_with_entity_child.yaml"
        );

        gen.generate(content.getBlockTypeFactory(), mapContext, worldContext, fluidContext);
        MapEntityDefinition player = gen.getEntities().get(0);

        ComponentDefinition pos = player.components().stream()
            .filter(c -> c.type() == Position.class)
            .findFirst()
            .orElseThrow();

        // leaf_with_entity's player at local (1,1,0), placed at world (5,0,0)
        // → world (6,1,0).
        assertEquals(6, asInt(pos.properties().get("x")));
        assertEquals(1, asInt(pos.properties().get("y")));
        assertEquals(0, asInt(pos.properties().get("z")));
    }

    private static int asInt(Object value) {
        return ((Number) value).intValue();
    }
}
