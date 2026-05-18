package ca.kieve.ssss.content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map YAML schema. All three entity-shaped lists ({@code entities},
 * {@code connectors}, {@code submaps}) share the same record type so
 * the editor can treat them uniformly. They are kept in separate
 * sections because the game loader processes each one differently:
 * <ul>
 *   <li>{@code entities} → ECS entities created by
 *       {@link EntityFactory}.</li>
 *   <li>{@code connectors} → load-time anchors consumed by the
 *       composite map loader; never instantiated as ECS entities.</li>
 *   <li>{@code submaps} → load-time region references consumed by the
 *       composite map loader; never instantiated as ECS entities.</li>
 * </ul>
 *
 * <p>The {@code id} field on a {@code MapEntityDefinition} carries
 * different semantics per list: a blueprint reference in
 * {@code entities}, and an instance name in {@code connectors} /
 * {@code submaps}.
 */
public record MapDefinition(
    Map<String, MapBlockDefinition> blocks,
    Map<String, String> layers,
    String floorGlyph,
    List<MapEntityDefinition> entities,
    List<MapEntityDefinition> connectors,
    List<MapEntityDefinition> submaps
) {
    public MapDefinition {
        blocks = blocks != null ? blocks : new HashMap<>();
        layers = layers != null ? layers : new HashMap<>();
        entities = entities != null ? entities : List.of();
        connectors = connectors != null ? connectors : List.of();
        submaps = submaps != null ? submaps : List.of();
    }
}
