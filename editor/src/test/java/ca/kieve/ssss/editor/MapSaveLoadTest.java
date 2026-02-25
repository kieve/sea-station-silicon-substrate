package ca.kieve.ssss.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.editor.model.EditorEntity;
import ca.kieve.ssss.editor.model.EditorMapModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class MapSaveLoadTest {
    @Test
    void roundTrip_staticTestMap(@TempDir Path tempDir) throws IOException {
        // Load the original map from classpath
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("content/maps/static_test_map.yaml");
        assertNotNull(stream, "static_test_map.yaml not on classpath");
        MapDefinition originalDef = MapLoader.load(stream);

        // Convert to editor model
        EditorMapModel model =
                EditorMapModel.fromDefinition(originalDef, null);

        // Save to temp file
        File tempFile = tempDir.resolve("saved_map.yaml").toFile();
        MapSaver.save(model, tempFile);

        // Reload the saved file
        MapDefinition savedDef = MapLoader.load(tempFile);
        EditorMapModel reloaded =
                EditorMapModel.fromDefinition(savedDef, tempFile);

        // Assert blocks match (name -> bpId)
        Map<String, MapBlockDefinition> origBlocks = model.getBlocks();
        Map<String, MapBlockDefinition> savedBlocks = reloaded.getBlocks();
        assertEquals(origBlocks.size(), savedBlocks.size(),
                "block count mismatch");
        for (var entry : origBlocks.entrySet()) {
            var savedBlock = savedBlocks.get(entry.getKey());
            assertNotNull(savedBlock, "missing block: " + entry.getKey());
            assertEquals(entry.getValue().bpId(), savedBlock.bpId(),
                    "bpId mismatch for block: " + entry.getKey());
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
            assertEquals(origGrid.getCells().size(),
                    savedGrid.getCells().size(),
                    "cell count mismatch at z=" + z);
            for (var cell : origGrid.getCells().entrySet()) {
                String savedVal = savedGrid.getCell(
                        cell.getKey().row(), cell.getKey().col());
                assertEquals(cell.getValue(), savedVal,
                        "cell mismatch at z=" + z
                                + " row=" + cell.getKey().row()
                                + " col=" + cell.getKey().col());
            }
        }

        // Assert entities match
        List<EditorEntity> origEntities = model.getEntities();
        List<EditorEntity> savedEntities = reloaded.getEntities();
        assertEquals(origEntities.size(), savedEntities.size(),
                "entity count mismatch");
        for (int i = 0; i < origEntities.size(); i++) {
            var origEntity = origEntities.get(i);
            var savedEntity = savedEntities.get(i);
            assertEquals(origEntity.id(), savedEntity.id(),
                    "entity id mismatch at index " + i);
            assertEquals(origEntity.components().size(),
                    savedEntity.components().size(),
                    "component count mismatch for entity "
                            + origEntity.id() + " at index " + i);
            for (int j = 0; j < origEntity.components().size(); j++) {
                var origComp = origEntity.components().get(j);
                var savedComp = savedEntity.components().get(j);
                assertEquals(origComp.type(), savedComp.type(),
                        "component type mismatch");
                assertEquals(origComp.properties(), savedComp.properties(),
                        "properties mismatch for "
                                + origComp.type().getSimpleName()
                                + " on entity " + origEntity.id());
            }
        }
    }
}
