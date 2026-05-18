package ca.kieve.ssss.editor.model;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.Connector;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.content.map.ConnectorDirection;
import ca.kieve.ssss.util.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
