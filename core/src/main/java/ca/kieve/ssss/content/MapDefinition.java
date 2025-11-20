package ca.kieve.ssss.content;

import ca.kieve.ssss.util.Vec3i;

import java.util.HashMap;
import java.util.Map;

public record MapDefinition(
    Map<String, MapBlockDefinition> blocks,
    MapSizeDefinition size,
    Map<String, String> layers,
    Vec3i playerSpawn,
    String floorGlyph
) {
    public MapDefinition {
        blocks = blocks != null ? blocks : new HashMap<>();
        layers = layers != null ? layers : new HashMap<>();
    }
}
