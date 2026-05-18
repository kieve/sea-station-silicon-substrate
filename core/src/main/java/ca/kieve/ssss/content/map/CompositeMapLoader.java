package ca.kieve.ssss.content.map;

import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import ca.kieve.ssss.component.Connector;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Submap;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRef;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.util.Vec3i;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads a root YAML and walks its submap references recursively, resolving
 * each placement's world-space offset.
 *
 * <p>Two placement modes are supported:
 * <ul>
 *   <li><b>Offset mode</b> — the submap entity carries a {@link Position}
 *       component that gives the child's offset relative to the parent.</li>
 *   <li><b>Connector mode</b> — the {@link Submap} component's
 *       {@code localConnector} names a connector in the parent and
 *       {@code remoteConnector} names one in the child;
 *       the child offset is computed so the child's connector lands on the
 *       parent's connector.</li>
 * </ul>
 *
 * <p>Cycles in the reference graph are detected; the same canonical path
 * appearing twice on an ancestor chain throws
 * {@link IllegalStateException}.
 *
 * <p>This class is pure resolution — it doesn't allocate a
 * {@link ca.kieve.ssss.world.WorldModel} or stamp any cells. Callers
 * consume the returned {@link Placement} list to perform world
 * construction.
 */
public class CompositeMapLoader {
    /**
     * One resolved leaf of the load tree.
     *
     * @param id short id for this region (defaults to {@link #sourcePath} minus extension)
     * @param sourcePath canonical path under {@code maps/}
     * @param definition the parsed YAML
     * @param worldOffset the region's {@code (0,0,0)} in final world coords
     * @param localBounds the region's extent in its own coordinate space (width/height/depth)
     * @param allowOverlap whether this placement may overwrite cells already claimed by another
     *                     region
     */
    public record Placement(
        String id,
        String sourcePath,
        MapDefinition definition,
        Vec3i worldOffset,
        Vec3i localBounds,
        boolean allowOverlap
    ) {
    }

    /**
     * Resolved view of a submap entity: pulled-out fields from the
     * Submap component, plus the (optional) Position component that
     * signals offset mode.
     */
    private record SubmapView(
        String ref,
        Vec3i offset,
        String localConnector,
        String remoteConnector,
        boolean allowOverlap
    ) {
    }

    /**
     * Resolved view of a connector entity: name (from the entity id),
     * position (required), direction (optional).
     */
    private record ConnectorView(String id, Vec3i position, ConnectorDirection direction) {
    }

    private static final String CONTENT_MAPS_PREFIX = "content/maps/";

    private final ObjectMapper m_yamlMapper;

    public CompositeMapLoader() {
        // Allow `direction: East`, `direction: east`, etc. — YAML authors
        // shouldn't have to match enum SHOUTCASE.
        m_yamlMapper = JsonMapper.builder(new YAMLFactory())
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .findAndAddModules()
            .build();
    }

    /**
     * Loads {@code rootPath} (relative to {@code content/maps/}) and every
     * submap it references, recursively. Returns the flat list of
     * placements in pre-order (parent before children).
     */
    public List<Placement> load(String rootPath) {
        String canonicalRoot = ContentRef.stripYamlSuffix(rootPath);
        MapDefinition rootDef = parseYaml(rootPath);
        List<Placement> out = new ArrayList<>();
        Set<String> ancestors = new LinkedHashSet<>();
        loadRecursive(rootPath, canonicalRoot, rootDef, Vec3i.ZERO, false, ancestors, out);
        validateNonNegative(out);
        validateUniqueIds(out);
        return out;
    }

    private void loadRecursive(
        String path,
        String regionId,
        MapDefinition def,
        Vec3i worldOffset,
        boolean allowOverlap,
        Set<String> ancestors,
        List<Placement> out
    ) {
        if (ancestors.contains(path)) {
            throw new IllegalStateException(
                "submap cycle detected: " + String.join(" -> ", ancestors) + " -> " + path
            );
        }

        Vec3i localBounds = computeBounds(def);
        out.add(new Placement(regionId, path, def, worldOffset, localBounds, allowOverlap));

        ancestors.add(path);
        try {
            for (MapEntityDefinition submapEntity : def.submaps()) {
                SubmapView submap = readSubmap(submapEntity, path);
                String childPath = ContentRef.resolve(toContentPath(path), submap.ref());
                String childMapPath = stripMapsPrefix(childPath);
                MapDefinition childDef = parseYaml(childMapPath);
                Vec3i childWorldOffset = worldOffset.add(
                    resolveSubmapOffset(def, submap, childDef, path)
                );
                String childId = submapEntity.id() != null
                    ? submapEntity.id()
                    : ContentRef.stripYamlSuffix(childMapPath);
                loadRecursive(
                    childMapPath,
                    childId,
                    childDef,
                    childWorldOffset,
                    submap.allowOverlap(),
                    ancestors,
                    out
                );
            }
        } finally {
            ancestors.remove(path);
        }
    }

    private static SubmapView readSubmap(MapEntityDefinition entity, String parentPath) {
        Submap sm = (Submap) findComponentInstance(entity, Submap.class);
        if (sm == null) {
            throw new IllegalArgumentException(
                "submap entity '" + entity.id() + "' in " + parentPath
                    + " is missing a Submap component"
            );
        }
        if (sm.ref == null || sm.ref.isEmpty()) {
            throw new IllegalArgumentException(
                "submap entity '" + entity.id() + "' in " + parentPath
                    + " has no ref"
            );
        }
        Vec3i offset = Position.readFromDefinition(entity);
        boolean hasConnectorPair = sm.localConnector != null && sm.remoteConnector != null;
        boolean partialConnectors = (sm.localConnector != null) != (sm.remoteConnector != null);
        if (partialConnectors) {
            throw new IllegalArgumentException(
                "submap '" + entity.id() + "' in " + parentPath
                    + ": both localConnector and remoteConnector must be set together"
            );
        }
        boolean offsetMode = offset != null;
        if (offsetMode == hasConnectorPair) {
            throw new IllegalArgumentException(
                "submap '" + entity.id() + "' in " + parentPath
                    + ": must specify exactly one of Position component or "
                    + "(localConnector + remoteConnector)"
            );
        }
        return new SubmapView(
            sm.ref,
            offset,
            sm.localConnector,
            sm.remoteConnector,
            sm.allowOverlap
        );
    }

    private Vec3i resolveSubmapOffset(
        MapDefinition parentDef,
        SubmapView submap,
        MapDefinition childDef,
        String parentPath
    ) {
        if (submap.offset() != null) {
            return submap.offset();
        }
        ConnectorView parentConnector = findConnector(
            parentDef,
            submap.localConnector(),
            parentPath
        );
        ConnectorView childConnector = findConnector(
            childDef,
            submap.remoteConnector(),
            submap.ref()
        );
        if (parentConnector.direction() == null) {
            throw new IllegalArgumentException(
                "connector '" + parentConnector.id() + "' in " + parentPath
                    + " has no direction; required when used as a localConnector"
            );
        }
        return parentConnector.position()
            .add(parentConnector.direction().unitVector())
            .subtract(childConnector.position());
    }

    private static ConnectorView findConnector(MapDefinition def, String id, String mapPath) {
        for (MapEntityDefinition entity : def.connectors()) {
            if (!entity.id().equals(id)) {
                continue;
            }
            Vec3i pos = Position.readFromDefinition(entity);
            if (pos == null) {
                throw new IllegalArgumentException(
                    "connector '" + id + "' in " + mapPath + " is missing a Position component"
                );
            }
            Connector conn = (Connector) findComponentInstance(entity, Connector.class);
            ConnectorDirection direction = conn != null ? conn.direction : null;
            return new ConnectorView(id, pos, direction);
        }
        throw new IllegalArgumentException("connector '" + id + "' not found in " + mapPath);
    }

    /**
     * Returns a newly-instantiated component of {@code type} from the
     * entity's components, or {@code null} if none. Used for read-only
     * field extraction from non-Position engine components.
     */
    private static Object findComponentInstance(MapEntityDefinition entity, Class<?> type) {
        for (ComponentDefinition comp : entity.components()) {
            if (comp.type() != type) {
                continue;
            }
            return instantiate(comp);
        }
        return null;
    }

    private static Object instantiate(ComponentDefinition def) {
        try {
            Object instance = def.type().getDeclaredConstructor().newInstance();
            for (var entry : def.properties().entrySet()) {
                try {
                    var field = def.type().getField(entry.getKey());
                    Object value = entry.getValue();
                    if (field.getType().isEnum() && value instanceof String s) {
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        Object enumVal = Enum
                            .valueOf((Class<Enum>) field.getType(), s.toUpperCase());
                        field.set(instance, enumVal);
                    } else if (field.getType() == boolean.class && value instanceof String s) {
                        field.set(instance, Boolean.parseBoolean(s));
                    } else if (field.getType() == int.class && value instanceof Number n) {
                        field.setInt(instance, n.intValue());
                    } else {
                        field.set(instance, value);
                    }
                } catch (NoSuchFieldException ignored) {
                    // Field doesn't exist on the component — skip silently.
                }
            }
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                "Failed to instantiate engine component " + def.type().getSimpleName(),
                e
            );
        }
    }

    private static Vec3i computeBounds(MapDefinition def) {
        if (def.layers().isEmpty()) {
            return new Vec3i(0, 0, 0);
        }
        int width = 0;
        int height = 0;
        int depth = def.layers().size();
        for (String layerData : def.layers().values()) {
            String[] lines = layerData.split("\n");
            height = Math.max(height, lines.length);
            for (String line : lines) {
                width = Math.max(width, line.length());
            }
        }
        return new Vec3i(width, height, depth);
    }

    private static void validateNonNegative(List<Placement> placements) {
        for (Placement p : placements) {
            if (p.worldOffset.x < 0 || p.worldOffset.y < 0 || p.worldOffset.z < 0) {
                throw new IllegalStateException(
                    "submap '" + p.id + "' resolved to negative world offset "
                        + p.worldOffset + "; negative world coordinates are not yet supported"
                );
            }
        }
    }

    private static void validateUniqueIds(List<Placement> placements) {
        Set<String> seen = new HashSet<>();
        for (Placement p : placements) {
            if (!seen.add(p.id)) {
                throw new IllegalStateException(
                    "duplicate region id '" + p.id
                        + "'; set submap.id explicitly when loading the same file twice"
                );
            }
        }
    }

    private MapDefinition parseYaml(String mapPath) {
        try {
            String yaml = Gdx.files.internal(CONTENT_MAPS_PREFIX + mapPath).readString();
            return m_yamlMapper.readValue(yaml, MapDefinition.class);
        } catch (IOException e) {
            throw new RuntimeException("failed to load map file: " + mapPath, e);
        }
    }

    private static String toContentPath(String mapPath) {
        return "maps/" + mapPath;
    }

    private static String stripMapsPrefix(String contentPath) {
        String prefix = "maps/";
        if (!contentPath.startsWith(prefix)) {
            throw new IllegalArgumentException(
                "submap refs must resolve under maps/, got: " + contentPath
            );
        }
        return contentPath.substring(prefix.length());
    }
}
