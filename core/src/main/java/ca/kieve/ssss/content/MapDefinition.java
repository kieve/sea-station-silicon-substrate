package ca.kieve.ssss.content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record MapDefinition(
    Map<String, MapBlockDefinition> blocks,
    Map<String, String> layers,
    String floorGlyph,
    List<MapEntityDefinition> entities
) {
    public MapDefinition {
        blocks = blocks != null ? blocks : new HashMap<>();
        layers = layers != null ? layers : new HashMap<>();
        entities = entities != null ? entities : List.of();
    }
}
