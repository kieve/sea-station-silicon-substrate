package ca.kieve.ssss.editor.model;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.Connector;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Submap;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.content.map.ConnectorDirection;
import ca.kieve.ssss.util.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComposedWorldTest {
    @Test
    void connectorOffsetLandsChildOneCellPastParentInDirection() {
        // Parent has a connector at (10, 5, 1) facing east. The child's
        // remote connector is at (0, 5, 1). The child should land so its
        // (0, 5, 1) sits one cell east of the parent's (10, 5, 1) —
        // i.e. at (11, 5, 1). The child's region offset is therefore
        // (11 - 0, 5 - 5, 1 - 1) = (11, 0, 0).
        var parent = List.of(connectorEntity("east", 10, 5, 1, ConnectorDirection.EAST));
        var child = List.of(connectorEntity("west", 0, 5, 1, null));

        Vec3i offset = ComposedWorld.computeConnectorOffset(parent, child, "east", "west");

        assertEquals(new Vec3i(11, 0, 0), offset);
    }

    @Test
    void connectorOffsetReturnsNullWhenDirectionMissing() {
        // Parent connector lacks a direction. The math is ambiguous in
        // that state, so the helper refuses to guess.
        var parent = List.of(connectorEntity("door", 5, 5, 0, null));
        var child = List.of(connectorEntity("door", 0, 0, 0, null));

        Vec3i offset = ComposedWorld.computeConnectorOffset(parent, child, "door", "door");

        assertNull(offset);
    }

    @Test
    void connectorOffsetReturnsNullWhenConnectorMissing() {
        var parent = List.<MapEntityDefinition>of();
        var child = List.of(connectorEntity("east", 0, 0, 0, null));

        assertNull(
            ComposedWorld.computeConnectorOffset(parent, child, "east", "east"),
            "missing local connector should return null"
        );
        assertNull(
            ComposedWorld.computeConnectorOffset(child, parent, "east", "east"),
            "missing remote connector should return null"
        );
    }

    private static MapEntityDefinition connectorEntity(
        String id,
        int x,
        int y,
        int z,
        ConnectorDirection direction
    ) {
        var components = new ArrayList<ComponentDefinition>();
        var connComp = new ComponentDefinition(Connector.class);
        if (direction != null) {
            connComp.setProperty("direction", direction.name().toLowerCase());
        }
        components.add(connComp);
        var posComp = new ComponentDefinition(Position.class);
        posComp.setProperty("x", x);
        posComp.setProperty("y", y);
        posComp.setProperty("z", z);
        components.add(posComp);
        return new MapEntityDefinition(id, components);
    }

    @Test
    void flattenStampsRootLayerWithoutOverlap() {
        // Single-file flatten: one region of solid wall blocks.
        var blocks = Map.of("wall", new MapBlockDefinition("block_wall", '#'));
        var layers = Map.of("0", "###\n###\n");
        var def = new MapDefinition(blocks, layers, "interpunct", List.of(), List.of(), List.of());

        ComposedWorld world = ComposedWorld.flatten(def, null);

        assertEquals(6, world.cellsAt(0).size(), "all six wall cells should be stamped");
        assertTrue(world.overlapCells().isEmpty(), "no overlaps in single-region map");
        assertEquals(1, world.regions().size());
        assertEquals(new Vec3i(3, 2, 1), world.regions().getFirst().bounds());
    }

    @Test
    void submapRegionsAtIgnoresRootOwnedCells() {
        var blocks = Map.of("wall", new MapBlockDefinition("block_wall", '#'));
        var layers = Map.of("0", "##\n");
        var def = new MapDefinition(blocks, layers, "interpunct", List.of(), List.of(), List.of());

        ComposedWorld world = ComposedWorld.flatten(def, null);

        assertTrue(world.submapRegionsAt(0, 0).isEmpty(), "root cells belong to no submap");
        assertTrue(world.submapRegionsAt(9, 9).isEmpty(), "cell outside every region");
    }

    @Test
    void regionCoversEveryCellInItsBoundsIncludingAir() {
        var region = region(new Vec3i(10, 4, 0), new Vec3i(3, 2, 1));

        assertTrue(ComposedWorld.covers(region, 4, 10), "top-left corner");
        assertTrue(ComposedWorld.covers(region, 5, 12), "bottom-right corner");
        assertTrue(ComposedWorld.covers(region, 4, 11), "interior cell");

        assertFalse(ComposedWorld.covers(region, 4, 9), "one column west");
        assertFalse(ComposedWorld.covers(region, 4, 13), "one column east");
        assertFalse(ComposedWorld.covers(region, 3, 10), "one row below");
        assertFalse(ComposedWorld.covers(region, 6, 10), "one row above");
    }

    @Test
    void emptyRegionCoversNothing() {
        var region = region(Vec3i.ZERO, Vec3i.ZERO);

        assertFalse(ComposedWorld.covers(region, 0, 0));
    }

    private static ComposedWorld.RegionInfo region(Vec3i offset, Vec3i bounds) {
        return new ComposedWorld.RegionInfo(
            "east_wing",
            ComposedWorld.ROOT_REGION_ID,
            offset,
            bounds,
            List.of(),
            Set.of(0)
        );
    }

    @Test
    void rootChildAncestorResolvesNestedRegionToTopLevelSubmap() {
        var parents = Map.of("east_wing", ComposedWorld.ROOT_REGION_ID, "pump_room", "east_wing");

        assertEquals("east_wing", ComposedWorld.rootChildAncestor(parents, "pump_room"));
        assertEquals("east_wing", ComposedWorld.rootChildAncestor(parents, "east_wing"));
    }

    @Test
    void rootChildAncestorReturnsNullWhenChainNeverReachesRoot() {
        var parents = Map.of("orphan", "detached_parent");

        assertNull(
            ComposedWorld.rootChildAncestor(parents, "orphan"),
            "a chain that never reaches root names nothing selectable"
        );
        assertNull(
            ComposedWorld.rootChildAncestor(parents, ComposedWorld.ROOT_REGION_ID),
            "the root is not one of its own submaps"
        );
        assertNull(
            ComposedWorld.rootChildAncestor(Map.of("a", "b", "b", "a"), "a"),
            "a parent cycle terminates instead of spinning"
        );
    }

    @Test
    void regionIdFallsBackToRefWhenSubmapHasNoId() {
        var withId = submapEntity("east_wing", "maintenance_sub.yaml");
        var withoutId = submapEntity(null, "maintenance_sub.yaml");

        assertEquals("east_wing", ComposedWorld.regionIdFor(withId));
        assertEquals("maintenance_sub", ComposedWorld.regionIdFor(withoutId));
    }

    private static MapEntityDefinition submapEntity(String id, String ref) {
        var submap = new ComponentDefinition(Submap.class);
        submap.setProperty("ref", ref);
        return new MapEntityDefinition(id, List.of(submap));
    }

    @Test
    void flattenSkipsAirCells() {
        // Layout char 'a' = air, '#' = wall. Only the wall cell should
        // be stamped — air cells are skipped so they don't create
        // bogus overlap reports.
        var blocks = Map.of(
            "air",
            new MapBlockDefinition("air", 'a'),
            "wall",
            new MapBlockDefinition("block_wall", '#')
        );
        var layers = Map.of("0", "a#\n");
        var def = new MapDefinition(blocks, layers, "interpunct", List.of(), List.of(), List.of());

        ComposedWorld world = ComposedWorld.flatten(def, null);

        assertEquals(1, world.cellsAt(0).size());
        assertEquals("block_wall", world.cellsAt(0).getFirst().bpId());
        assertFalse(world.overlapCells().contains(new ComposedWorld.CellPos(0, 0, 0)));
    }
}
