package ca.kieve.ssss.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.content.ContentLoader;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.map.YamlMapGenerator;
import ca.kieve.ssss.context.MapContext;
import ca.kieve.ssss.context.WorldContext;
import ca.kieve.ssss.testharness.HeadlessGdxBootstrap;
import ca.kieve.ssss.util.Vec3i;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionGraphTest {
    private ContentRegistry content;
    private MapContext mapContext;
    private WorldModel world;

    @BeforeEach
    void setUp() {
        HeadlessGdxBootstrap.ensureInitialized();
        content = new ContentLoader().loadAll();
        mapContext = new MapContext();
    }

    @Test
    void discoversDoorPortalBetweenHomeBaseSubs() {
        loadComposite("home_base/sub_complex.yaml");

        RegionGraph graph = RegionGraph.discover(world, mapContext);

        // The two regions touch only at the open door cell (11, 5, 1).
        // East discovery sees (11, 5, 1) → (12, 5, 1) as a portal pair.
        assertEquals(1, graph.portals().size(), graph.portals().toString());
        Portal p = graph.portals().get(0);
        assertEquals("damaged_sub_root", p.regionA());
        assertEquals(new Vec3i(11, 5, 1), p.cellInA());
        assertEquals("maintenance_east", p.regionB());
        assertEquals(new Vec3i(12, 5, 1), p.cellInB());
    }

    @Test
    void portalsForReturnsSamePortalFromBothEndpoints() {
        loadComposite("home_base/sub_complex.yaml");

        RegionGraph graph = RegionGraph.discover(world, mapContext);

        assertEquals(1, graph.portalsFor("damaged_sub_root").size());
        assertEquals(1, graph.portalsFor("maintenance_east").size());
        assertEquals(
            graph.portalsFor("damaged_sub_root").get(0),
            graph.portalsFor("maintenance_east").get(0)
        );
    }

    @Test
    void findPortalTowardsAdjacentRegionReturnsTheOnlyPortal() {
        loadComposite("home_base/sub_complex.yaml");

        RegionGraph graph = RegionGraph.discover(world, mapContext);

        Portal viaPortal = graph.findPortalTowards(
            "damaged_sub_root",
            "maintenance_east",
            new Vec3i(7, 5, 1)
        );
        assertNotNull(viaPortal);
        // Returned portal must be one this region actually touches
        assertTrue(graph.portalsFor("damaged_sub_root").contains(viaPortal));
    }

    @Test
    void findPortalTowardsSameRegionReturnsNull() {
        loadComposite("home_base/sub_complex.yaml");

        RegionGraph graph = RegionGraph.discover(world, mapContext);

        assertNull(
            graph.findPortalTowards("damaged_sub_root", "damaged_sub_root", new Vec3i(7, 5, 1))
        );
    }

    @Test
    void findPortalTowardsUnreachableRegionReturnsNull() {
        loadComposite("home_base/sub_complex.yaml");

        RegionGraph graph = RegionGraph.discover(world, mapContext);

        assertNull(
            graph.findPortalTowards("damaged_sub_root", "nope/doesnt_exist", new Vec3i(7, 5, 1))
        );
    }

    @Test
    void neighborsOfReturnsAdjacentRegions() {
        loadComposite("home_base/sub_complex.yaml");

        RegionGraph graph = RegionGraph.discover(world, mapContext);

        assertEquals(Set.of("maintenance_east"), graph.neighborsOf("damaged_sub_root"));
        assertEquals(Set.of("damaged_sub_root"), graph.neighborsOf("maintenance_east"));
    }

    @Test
    void portalsBetweenSymmetricallyReturnsEdgePortals() {
        loadComposite("home_base/sub_complex.yaml");

        RegionGraph graph = RegionGraph.discover(world, mapContext);

        var fromA = graph.portalsBetween("damaged_sub_root", "maintenance_east");
        var fromB = graph.portalsBetween("maintenance_east", "damaged_sub_root");
        assertEquals(1, fromA.size());
        assertEquals(fromA, fromB);
    }

    @Test
    void multiPortalEdgePicksClosestToFromCell() {
        // wide_portal.yaml: two 2x3x2 leaves side by side with three
        // adjacent open columns at z=1 → three portals between them.
        loadComposite("test/composite/wide_portal.yaml");

        RegionGraph graph = RegionGraph.discover(world, mapContext);

        var portalsBetween = graph.portalsBetween("east", "west");
        assertEquals(3, portalsBetween.size(), portalsBetween.toString());

        // Start at the south end of the east region — closest portal is y=0.
        Portal south = graph.findPortalTowards("east", "west", new Vec3i(1, 0, 1));
        assertNotNull(south);
        assertEquals(new Vec3i(1, 0, 1), south.cellIn("east"));

        // Start at the north end — closest portal is y=2.
        Portal north = graph.findPortalTowards("east", "west", new Vec3i(1, 2, 1));
        assertNotNull(north);
        assertEquals(new Vec3i(1, 2, 1), north.cellIn("east"));
    }

    private void loadComposite(String mapPath) {
        YamlMapGenerator gen = new YamlMapGenerator(mapPath);
        WorldContext worldContext = new WorldContext();
        gen.generate(content.getBlockTypeFactory(), mapContext, worldContext);
        world = worldContext.getModel();
    }
}
