package ca.kieve.ssss.editor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.kieve.ssss.component.Connector;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Submap;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.content.map.ConnectorDirection;
import ca.kieve.ssss.editor.model.EditorEntity;
import ca.kieve.ssss.editor.model.EditorMapModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapSaveLoadTest {
    @Test
    void roundTrip_staticTestMap(@TempDir Path tempDir) throws IOException {
        // Load the original map from classpath
        InputStream stream = getClass().getClassLoader()
            .getResourceAsStream("content/maps/debug/static_test_map.yaml");
        assertNotNull(stream, "static_test_map.yaml not on classpath");
        MapDefinition originalDef = MapLoader.load(stream);

        // Convert to editor model
        EditorMapModel model = EditorMapModel.fromDefinition(originalDef, null);

        // Save to temp file
        File tempFile = tempDir.resolve("saved_map.yaml").toFile();
        MapSaver.save(model, tempFile);

        // Reload the saved file
        MapDefinition savedDef = MapLoader.load(tempFile);
        EditorMapModel reloaded = EditorMapModel.fromDefinition(savedDef, tempFile);

        // Assert blocks match (name -> bpId)
        Map<String, MapBlockDefinition> origBlocks = model.getBlocks();
        Map<String, MapBlockDefinition> savedBlocks = reloaded.getBlocks();
        assertEquals(origBlocks.size(), savedBlocks.size(), "block count mismatch");
        for (var entry : origBlocks.entrySet()) {
            var savedBlock = savedBlocks.get(entry.getKey());
            assertNotNull(savedBlock, "missing block: " + entry.getKey());
            assertEquals(
                entry.getValue().bpId(),
                savedBlock.bpId(),
                "bpId mismatch for block: " + entry.getKey()
            );
        }

        // Assert floor glyph matches
        assertEquals(model.getFloorGlyph(), reloaded.getFloorGlyph());

        // Assert layers match cell-by-cell
        List<Integer> origLevels = model.getZLevels();
        List<Integer> savedLevels = reloaded.getZLevels();
        assertEquals(origLevels, savedLevels, "z-level mismatch");

        for (int z : origLevels) {
            var origGrid = model.getLayer(z);
            var savedGrid = reloaded.getLayer(z);
            assertNotNull(savedGrid, "missing layer z=" + z);
            assertEquals(
                origGrid.getCells().size(),
                savedGrid.getCells().size(),
                "cell count mismatch at z=" + z
            );
            for (var cell : origGrid.getCells().entrySet()) {
                String savedVal = savedGrid.getCell(cell.getKey().row(), cell.getKey().col());
                assertEquals(
                    cell.getValue(),
                    savedVal,
                    "cell mismatch at z=" + z
                        + " row=" + cell.getKey().row()
                        + " col=" + cell.getKey().col()
                );
            }
        }

        // Assert entities match
        List<EditorEntity> origEntities = model.getEntities();
        List<EditorEntity> savedEntities = reloaded.getEntities();
        assertEquals(origEntities.size(), savedEntities.size(), "entity count mismatch");
        for (int i = 0; i < origEntities.size(); i++) {
            var origEntity = origEntities.get(i);
            var savedEntity = savedEntities.get(i);
            assertEquals(origEntity.id(), savedEntity.id(), "entity id mismatch at index " + i);
            assertEquals(
                origEntity.components().size(),
                savedEntity.components().size(),
                "component count mismatch for entity "
                    + origEntity.id() + " at index " + i
            );
            for (int j = 0; j < origEntity.components().size(); j++) {
                var origComp = origEntity.components().get(j);
                var savedComp = savedEntity.components().get(j);
                assertEquals(origComp.type(), savedComp.type(), "component type mismatch");
                assertEquals(
                    origComp.properties(),
                    savedComp.properties(),
                    "properties mismatch for "
                        + origComp.type().getSimpleName()
                        + " on entity " + origEntity.id()
                );
            }
        }
    }

    @Test
    void compositionOnlyFileGetsSyntheticAnchorLayer() {
        // A composition root (no `layers:`) used to crash MapViewPanel
        // because getZLevels().getFirst() ran on an empty list. The model
        // now synthesizes z=0 so the UI always has something to anchor on.
        var def = new MapDefinition(
            Map.of(),
            Map.of(),
            "interpunct",
            List.of(),
            List.of(),
            List.of(buildSubmapEntity("child", "./child.yaml", true))
        );

        var model = EditorMapModel.fromDefinition(def, null);

        assertEquals(List.of(0), model.getZLevels());
        assertTrue(model.getLayer(0).isEmpty(), "synthetic anchor layer should be empty");
        // Round-trip back to a definition — synthetic empty layer should
        // not be written back since the user never painted into it.
        MapDefinition out = model.toDefinition();
        assertTrue(out.layers().isEmpty(), "empty synthetic layer must not round-trip");
    }

    @Test
    void roundTrip_connectorsAndSubmaps(@TempDir Path tempDir) throws IOException {
        // Build a model with one block, two connectors (with and without
        // direction), and two submap placements (offset and connector pair).
        // Connectors and submaps are entity-shaped: id + components.
        var blocks = Map.of("wall", new MapBlockDefinition("block_wall", 'a'));
        var layers = Map.of("0", "aa\naa\n");
        var connectors = List.of(
            buildConnectorEntity("east_door", 1, 0, 0, ConnectorDirection.EAST),
            buildConnectorEntity("inbound", 0, 1, 0, null)
        );
        var submaps = List.of(
            buildSubmapEntityOffsetMode("explicit_neighbor", "./neighbor.yaml", 5, 0, 0, false),
            buildSubmapEntityConnectorMode(
                "via_connector",
                "/maps/test/other.yaml",
                "east_door",
                "west_door",
                true
            )
        );
        var def = new MapDefinition(blocks, layers, "interpunct", List.of(), connectors, submaps);
        var model = EditorMapModel.fromDefinition(def, null);

        File saved = tempDir.resolve("composition.yaml").toFile();
        MapSaver.save(model, saved);

        MapDefinition reloadedDef = MapLoader.load(saved);
        EditorMapModel reloaded = EditorMapModel.fromDefinition(reloadedDef, saved);

        assertEquals(connectors.size(), reloaded.getConnectors().size());
        var loadedEast = reloaded.getConnectors().get(0);
        assertEquals("east_door", loadedEast.id());
        assertEquals("EAST", readConnectorDirection(loadedEast));

        var loadedInbound = reloaded.getConnectors().get(1);
        assertEquals("inbound", loadedInbound.id());
        assertNull(readConnectorDirection(loadedInbound));

        assertEquals(submaps.size(), reloaded.getSubmaps().size());
        var loadedOffset = reloaded.getSubmaps().get(0);
        assertEquals("./neighbor.yaml", readSubmapField(loadedOffset, "ref"));
        assertEquals("explicit_neighbor", loadedOffset.id());
        assertNotNull(loadedOffset.getPositionComponent());
        assertNull(readSubmapField(loadedOffset, "localConnector"));

        var loadedConnectorMode = reloaded.getSubmaps().get(1);
        assertEquals("/maps/test/other.yaml", readSubmapField(loadedConnectorMode, "ref"));
        assertNull(loadedConnectorMode.getPositionComponent());
        assertEquals("east_door", readSubmapField(loadedConnectorMode, "localConnector"));
        assertEquals("west_door", readSubmapField(loadedConnectorMode, "remoteConnector"));
        assertEquals("true", String.valueOf(readSubmapField(loadedConnectorMode, "allowOverlap")));
    }

    private static MapEntityDefinition buildConnectorEntity(
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

    private static MapEntityDefinition buildSubmapEntity(
        String id,
        String ref,
        boolean withPosition
    ) {
        var components = new ArrayList<ComponentDefinition>();
        var sm = new ComponentDefinition(Submap.class);
        sm.setProperty("ref", ref);
        components.add(sm);
        if (withPosition) {
            var posComp = new ComponentDefinition(Position.class);
            posComp.setProperty("x", 0);
            posComp.setProperty("y", 0);
            posComp.setProperty("z", 0);
            components.add(posComp);
        }
        return new MapEntityDefinition(id, components);
    }

    private static MapEntityDefinition buildSubmapEntityOffsetMode(
        String id,
        String ref,
        int x,
        int y,
        int z,
        boolean allowOverlap
    ) {
        var components = new ArrayList<ComponentDefinition>();
        var sm = new ComponentDefinition(Submap.class);
        sm.setProperty("ref", ref);
        if (allowOverlap) {
            sm.setProperty("allowOverlap", true);
        }
        components.add(sm);
        var posComp = new ComponentDefinition(Position.class);
        posComp.setProperty("x", x);
        posComp.setProperty("y", y);
        posComp.setProperty("z", z);
        components.add(posComp);
        return new MapEntityDefinition(id, components);
    }

    private static MapEntityDefinition buildSubmapEntityConnectorMode(
        String id,
        String ref,
        String localConnector,
        String remoteConnector,
        boolean allowOverlap
    ) {
        var components = new ArrayList<ComponentDefinition>();
        var sm = new ComponentDefinition(Submap.class);
        sm.setProperty("ref", ref);
        sm.setProperty("localConnector", localConnector);
        sm.setProperty("remoteConnector", remoteConnector);
        if (allowOverlap) {
            sm.setProperty("allowOverlap", true);
        }
        components.add(sm);
        return new MapEntityDefinition(id, components);
    }

    private static String readSubmapField(EditorEntity entity, String fieldName) {
        for (ComponentDefinition comp : entity.components()) {
            if (comp.type() == Submap.class) {
                Object value = comp.properties().get(fieldName);
                return value == null ? null : value.toString();
            }
        }
        return null;
    }

    private static String readConnectorDirection(EditorEntity entity) {
        for (ComponentDefinition comp : entity.components()) {
            if (comp.type() == Connector.class) {
                Object value = comp.properties().get("direction");
                return value == null ? null : value.toString().toUpperCase();
            }
        }
        return null;
    }
}
