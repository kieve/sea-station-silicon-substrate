package ca.kieve.ssss.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.context.GameContext;
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

        int depth = m_mapDefinition.layers().size();
        int width = 0;
        int height = 0;
        for (String layerData : m_mapDefinition.layers().values()) {
            String[] lines = layerData.split("\n");
            height = Math.max(height, lines.length);
            for (String line : lines) {
                width = Math.max(width, line.length());
            }
        }

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

    @Override
    public void createEntities(GameContext context, Vec3i playerSpawn) {
        var factory = context.entityFactory();

        // Debug movers with different speeds
        factory.createDebugMover(context,
            playerSpawn.add(Vec3i.X.product(-2)),
            50,
            Color.BLUE
        );

        factory.createDebugMover(context,
            playerSpawn.add(Vec3i.X.product(2)),
            100,
            Color.WHITE
        );

        factory.createDebugMover(context,
            playerSpawn.add(Vec3i.X.product(4)),
            200,
            Color.RED
        );

        // Test socket for taking over dead entities
        factory.createEntity(context,
            "deadMech",
            new Vec3i(5, 5, 1),
            Color.GOLD
        );

        // Training dummy for combat testing (in room 2)
        factory.createEntity(context,
            "trainingDummy",
            new Vec3i(23, 7, 1),
            Color.PINK
        );

        // Attacker AI test
        factory.createEntity(context,
            "aiAttacker",
            playerSpawn.add(new Vec3i(3, 0, 0)),
            Color.SCARLET
        );

        // Pickable note items
        factory.createEntity(context, "note", new Vec3i(14, 14, 1), Color.WHITE);
        factory.createEntity(context, "greenNote", new Vec3i(14, 14, 1), Color.GREEN);
        factory.createEntity(context, "blueNote", new Vec3i(14, 14, 1), Color.BLUE);

        // Spawn RoboMouse away from walls with random CW/CCW and random direction
        Vec3i mouseSpawn = new Vec3i(8, 8, 1);
        boolean clockwise = context.random().nextBoolean();
        factory.createRoboMouse(context, mouseSpawn, clockwise, null, Color.GRAY);

        // Door and key for testing lockable/openable system
        factory.createEntity(context, "maintenanceSubDoor", new Vec3i(17, 7, 1));
        factory.createEntity(
            context, "maintenanceSubDoorKey", new Vec3i(8, 6, 1), Color.YELLOW);
    }
}
