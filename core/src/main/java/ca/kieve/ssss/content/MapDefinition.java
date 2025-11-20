package ca.kieve.ssss.content;

import java.util.HashMap;
import java.util.Map;

public record MapDefinition(
    Map<String, MapBlockDefinition> blocks,
    MapSizeDefinition size,
    Map<String, String> layers,
    int playerSpawnX,
    int playerSpawnY,
    int playerSpawnZ
) {
    public MapDefinition {
        blocks = blocks != null ? blocks : new HashMap<>();
        layers = layers != null ? layers : new HashMap<>();
    }
}
