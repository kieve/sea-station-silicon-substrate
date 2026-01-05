package ca.kieve.ssss.world;

import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.util.Vec3i;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A static map generator that loads map layout from YAML.
 * The map layout uses character-based representation for each Z-level,
 * with block type mappings defined in the YAML file.
 */
public class StaticTestMapGenerator implements MapGenerator {

    private static final String MAP_FILE = "content/maps/static_test_map.yaml";

    private MapDefinition m_mapDefinition;
    private Map<Character, String> m_charToBlockType;

    @Override
    public WorldModel generate(BlockTypeFactory blockTypeFactory) {
        loadMapDefinition();
        buildCharacterMapping();

        int width = m_mapDefinition.size().x();
        int height = m_mapDefinition.size().y();
        int depth = m_mapDefinition.layers().size();

        WorldModel world = new WorldModel(width, height, depth, blockTypeFactory);

        // Parse each layer and populate the world
        for (Map.Entry<String, String> layerEntry : m_mapDefinition.layers().entrySet()) {
            int z = Integer.parseInt(layerEntry.getKey());
            String layerData = layerEntry.getValue();
            parseLayer(world, layerData, z);
        }

        return world;
    }

    @Override
    public Vec3i getPlayerSpawn() {
        return m_mapDefinition.playerSpawn();
    }

    @Override
    public String getFloorGlyphId() {
        return m_mapDefinition.floorGlyph();
    }

    private void loadMapDefinition() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.findAndRegisterModules();
            String yamlContent = Gdx.files.internal(MAP_FILE).readString();
            m_mapDefinition = mapper.readValue(yamlContent, MapDefinition.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load map file: " + MAP_FILE, e);
        }
    }

    private void buildCharacterMapping() {
        m_charToBlockType = new HashMap<>();
        for (Map.Entry<String, MapBlockDefinition> entry
                : m_mapDefinition.blocks().entrySet()) {
            MapBlockDefinition blockDef = entry.getValue();
            m_charToBlockType.put(blockDef.layoutChar(), blockDef.type());
        }
    }

    private void parseLayer(WorldModel world, String layerData, int z) {
        String[] lines = layerData.split("\n");

        for (int y = 0; y < lines.length && y < world.getHeight(); y++) {
            String line = lines[y];
            for (int x = 0; x < line.length() && x < world.getWidth(); x++) {
                char c = line.charAt(x);
                String blockType = m_charToBlockType.get(c);
                if (blockType != null) {
                    world.setBlock(x, y, z, blockType);
                }
            }
        }
    }
}
