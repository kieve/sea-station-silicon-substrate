package ca.kieve.ssss.editor.model;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRef;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.content.map.ConnectorDirection;
import ca.kieve.ssss.editor.MapLoader;
import ca.kieve.ssss.editor.util.FileUtil;
import ca.kieve.ssss.editor.util.MapPathUtil;
import ca.kieve.ssss.util.Vec3i;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Read-only flattened view of a composition map plus all transitively
 * referenced submaps, in world coordinates. The editor produces one of
 * these on demand to render a composed-world preview and to report
 * overlap warnings.
 *
 * <p>Mirrors the resolution logic of the runtime
 * {@code CompositeMapLoader}, but uses {@link MapLoader} (file-based)
 * rather than {@code Gdx.files} so it can run inside the editor without
 * a libGDX context.
 *
 * <p>Connectors and submaps are read from {@link MapEntityDefinition}
 * lists on {@link MapDefinition}: the entity's {@code Position}
 * component gives the world-space cell (for connectors) or offset
 * (offset-mode submaps), and the {@code Connector} / {@code Submap}
 * components carry the remaining fields.
 */
public final class ComposedWorld {
    public record CellPos(int row, int col, int z) {
    }

    public record Cell(int row, int col, int z, String bpId, String regionId) {
    }

    /**
     * Entity from a submap, with its position translated into the parent's
     * world coordinate space.
     */
    public record Entity(
        String id,
        int row,
        int col,
        int z,
        String regionId,
        List<ComponentDefinition> components
    ) {
    }

    public record RegionInfo(
        String id,
        String parentId,
        Vec3i offset,
        Vec3i bounds,
        List<MapEntityDefinition> connectors,
        Set<Integer> declaredZLevels
    ) {
    }

    private static final class Context {
        final Map<CellPos, Cell> claimed = new HashMap<>();
        final Set<CellPos> overlapCells = new HashSet<>();
        final List<RegionInfo> regions = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final List<Entity> entities = new ArrayList<>();

        void loadRecursive(
            File file,
            MapDefinition def,
            String regionId,
            String parentId,
            Vec3i offset,
            boolean allowOverlap,
            Set<File> ancestors
        ) {
            if (file != null) {
                File canonical = FileUtil.canonicalOrAbsolute(file);
                if (ancestors.contains(canonical)) {
                    warnings.add("submap cycle detected at " + canonical.getName());
                    return;
                }
                ancestors.add(canonical);
            }
            try {
                Vec3i bounds = stampDefinition(def, regionId, offset, allowOverlap);
                Set<Integer> declaredZ = new TreeSet<>();
                for (String zKey : def.layers().keySet()) {
                    try {
                        declaredZ.add(Integer.parseInt(zKey) + offset.z);
                    } catch (NumberFormatException ignored) {
                        // skip non-numeric layer keys
                    }
                }
                regions.add(
                    new RegionInfo(
                        regionId,
                        parentId,
                        offset,
                        bounds,
                        def.connectors(),
                        Set.copyOf(declaredZ)
                    )
                );
                collectEntities(def, regionId, offset);

                for (MapEntityDefinition submapEntity : def.submaps()) {
                    String ref = readSubmapRef(submapEntity);
                    if (ref == null) {
                        warnings.add(
                            "submap '" + submapEntity.id() + "' is missing a Submap component"
                        );
                        continue;
                    }
                    File childFile = MapPathUtil.resolveSubmapRef(file, ref);
                    if (childFile == null || !childFile.isFile()) {
                        warnings.add("could not resolve submap ref: " + ref);
                        continue;
                    }
                    MapDefinition childDef;
                    try {
                        childDef = MapLoader.load(childFile);
                    } catch (IOException ex) {
                        warnings.add("failed to load submap: " + ref);
                        continue;
                    }
                    Vec3i childOffset = resolveOffset(def, submapEntity, childDef, offset);
                    if (childOffset == null) {
                        warnings.add("could not resolve offset for submap: " + ref);
                        continue;
                    }
                    loadRecursive(
                        childFile,
                        childDef,
                        regionIdFor(submapEntity),
                        regionId,
                        childOffset,
                        readAllowOverlap(submapEntity),
                        ancestors
                    );
                }
            } finally {
                if (file != null) {
                    ancestors.remove(FileUtil.canonicalOrAbsolute(file));
                }
            }
        }

        Vec3i stampDefinition(
            MapDefinition def,
            String regionId,
            Vec3i offset,
            boolean allowOverlap
        ) {
            Map<Character, String> charToBpId = new HashMap<>();
            for (var entry : def.blocks().entrySet()) {
                MapBlockDefinition blockDef = entry.getValue();
                charToBpId.put(blockDef.layoutChar(), blockDef.bpId());
            }

            int width = 0;
            int height = 0;
            int depth = def.layers().size();

            for (var layerEntry : def.layers().entrySet()) {
                int localZ = Integer.parseInt(layerEntry.getKey());
                String[] lines = layerEntry.getValue().split("\n");
                height = Math.max(height, lines.length);
                for (int localY = 0; localY < lines.length; localY++) {
                    String line = lines[localY];
                    width = Math.max(width, line.length());
                    for (int localX = 0; localX < line.length(); localX++) {
                        String bpId = charToBpId.get(line.charAt(localX));
                        if (bpId == null || "air".equals(bpId)) {
                            continue;
                        }
                        int worldRow = offset.y + localY;
                        int worldCol = offset.x + localX;
                        int worldZ = offset.z + localZ;
                        CellPos pos = new CellPos(worldRow, worldCol, worldZ);
                        Cell prior = claimed.get(pos);
                        Cell stamped = new Cell(worldRow, worldCol, worldZ, bpId, regionId);
                        if (prior != null && !prior.regionId().equals(regionId) && !allowOverlap) {
                            overlapCells.add(pos);
                        }
                        claimed.put(pos, stamped);
                    }
                }
            }
            return new Vec3i(width, height, depth);
        }

        void collectEntities(MapDefinition def, String regionId, Vec3i offset) {
            for (MapEntityDefinition entity : def.entities()) {
                Vec3i pos = Position.readFromDefinition(entity);
                if (pos == null) {
                    continue;
                }
                entities.add(
                    new Entity(
                        entity.id(),
                        pos.y + offset.y,
                        pos.x + offset.x,
                        pos.z + offset.z,
                        regionId,
                        entity.components()
                    )
                );
            }
        }

        Vec3i resolveOffset(
            MapDefinition parentDef,
            MapEntityDefinition submapEntity,
            MapDefinition childDef,
            Vec3i parentOffset
        ) {
            Vec3i ownOffset = Position.readFromDefinition(submapEntity);
            if (ownOffset != null) {
                return parentOffset.add(ownOffset);
            }
            String localId = readSubmapField(submapEntity, "localConnector");
            String remoteId = readSubmapField(submapEntity, "remoteConnector");
            if (localId == null || remoteId == null) {
                return null;
            }
            Vec3i localOffset = computeConnectorOffset(
                parentDef.connectors(),
                childDef.connectors(),
                localId,
                remoteId
            );
            if (localOffset == null) {
                return null;
            }
            return parentOffset.add(localOffset);
        }
    }

    public static final String ROOT_REGION_ID = "root";

    private final Map<Integer, List<Cell>> m_cellsByZ;
    private final Map<Integer, List<Entity>> m_entitiesByZ;
    private final Map<String, String> m_regionParents;
    private final Set<CellPos> m_overlapCells;
    private final List<RegionInfo> m_regions;
    private final List<String> m_warnings;

    private ComposedWorld(
        Map<Integer, List<Cell>> cellsByZ,
        Map<Integer, List<Entity>> entitiesByZ,
        Set<CellPos> overlapCells,
        List<RegionInfo> regions,
        List<String> warnings
    ) {
        m_cellsByZ = cellsByZ;
        m_entitiesByZ = entitiesByZ;
        m_overlapCells = overlapCells;
        m_regions = regions;
        m_warnings = warnings;

        Map<String, String> parents = new HashMap<>();
        for (RegionInfo region : regions) {
            if (region.parentId() != null) {
                parents.put(region.id(), region.parentId());
            }
        }
        m_regionParents = parents;
    }

    public List<Cell> cellsAt(int z) {
        return m_cellsByZ.getOrDefault(z, List.of());
    }

    public List<Entity> entitiesAt(int z) {
        return m_entitiesByZ.getOrDefault(z, List.of());
    }

    public Set<CellPos> overlapCells() {
        return m_overlapCells;
    }

    public Set<String> submapRegionsAt(int row, int col) {
        Set<String> out = new LinkedHashSet<>();
        for (RegionInfo region : m_regions) {
            if (!covers(region, row, col)) {
                continue;
            }
            String topLevel = rootChildAncestor(m_regionParents, region.id());
            if (topLevel != null) {
                out.add(topLevel);
            }
        }
        return out;
    }

    static boolean covers(RegionInfo region, int row, int col) {
        Vec3i offset = region.offset();
        Vec3i bounds = region.bounds();
        return col >= offset.x
            && col < offset.x + bounds.x
            && row >= offset.y
            && row < offset.y + bounds.y;
    }

    static String rootChildAncestor(Map<String, String> regionParents, String regionId) {
        String current = regionId;
        Set<String> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            String parent = regionParents.get(current);
            if (ROOT_REGION_ID.equals(parent)) {
                return current;
            }
            current = parent;
        }
        return null;
    }

    public List<RegionInfo> regions() {
        return m_regions;
    }

    public List<String> warnings() {
        return m_warnings;
    }

    /**
     * Union of declared z-levels across every region in the composition,
     * in ascending order. Useful for sizing a layer dropdown when the
     * local map has no layers of its own.
     */
    public List<Integer> allDeclaredZLevels() {
        Set<Integer> zs = new TreeSet<>();
        for (RegionInfo region : m_regions) {
            zs.addAll(region.declaredZLevels());
        }
        return List.copyOf(zs);
    }

    /**
     * Flattens {@code rootDef} (loaded from {@code rootFile}) along with
     * every transitively referenced submap.
     */
    public static ComposedWorld flatten(MapDefinition rootDef, File rootFile) {
        var ctx = new Context();
        ctx.loadRecursive(
            rootFile,
            rootDef,
            ROOT_REGION_ID,
            null,
            Vec3i.ZERO,
            false,
            new LinkedHashSet<>()
        );
        Map<Integer, List<Cell>> cellsByZ = new LinkedHashMap<>();
        for (Cell cell : ctx.claimed.values()) {
            cellsByZ.computeIfAbsent(cell.z(), k -> new ArrayList<>()).add(cell);
        }
        Map<Integer, List<Entity>> entitiesByZ = new LinkedHashMap<>();
        for (Entity entity : ctx.entities) {
            entitiesByZ.computeIfAbsent(entity.z(), k -> new ArrayList<>()).add(entity);
        }
        return new ComposedWorld(
            cellsByZ,
            entitiesByZ,
            Set.copyOf(ctx.overlapCells),
            List.copyOf(ctx.regions),
            List.copyOf(ctx.warnings)
        );
    }

    public static String regionIdFor(MapEntityDefinition submapEntity) {
        if (submapEntity.id() != null) {
            return submapEntity.id();
        }
        String ref = readSubmapRef(submapEntity);
        return ref == null ? null : ContentRef.stripYamlSuffix(ref);
    }

    /**
     * Computes the child region's offset for a connector-mode submap
     * placement, given the two connector entity lists and the names of
     * the local/remote connector entities. Returns {@code null} if
     * either named connector is missing or the local connector has no
     * direction.
     */
    public static Vec3i computeConnectorOffset(
        List<MapEntityDefinition> parentConnectors,
        List<MapEntityDefinition> childConnectors,
        String localId,
        String remoteId
    ) {
        MapEntityDefinition local = findConnectorById(parentConnectors, localId);
        MapEntityDefinition remote = findConnectorById(childConnectors, remoteId);
        if (local == null || remote == null) {
            return null;
        }
        Vec3i localPos = Position.readFromDefinition(local);
        Vec3i remotePos = Position.readFromDefinition(remote);
        ConnectorDirection direction = readConnectorDirection(local);
        if (localPos == null || remotePos == null || direction == null) {
            return null;
        }
        return localPos.add(direction.unitVector()).subtract(remotePos);
    }

    private static MapEntityDefinition findConnectorById(
        List<MapEntityDefinition> connectors,
        String id
    ) {
        if (id == null) {
            return null;
        }
        for (MapEntityDefinition c : connectors) {
            if (id.equals(c.id())) {
                return c;
            }
        }
        return null;
    }

    private static ConnectorDirection readConnectorDirection(MapEntityDefinition entity) {
        for (ComponentDefinition comp : entity.components()) {
            if (!"Connector".equals(comp.type().getSimpleName())) {
                continue;
            }
            Object value = comp.properties().get("direction");
            if (value == null) {
                return null;
            }
            try {
                return ConnectorDirection.valueOf(value.toString().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return null;
    }

    private static String readSubmapRef(MapEntityDefinition entity) {
        return readSubmapField(entity, "ref");
    }

    private static String readSubmapField(MapEntityDefinition entity, String fieldName) {
        for (ComponentDefinition comp : entity.components()) {
            if (!"Submap".equals(comp.type().getSimpleName())) {
                continue;
            }
            Object value = comp.properties().get(fieldName);
            return value == null ? null : value.toString();
        }
        return null;
    }

    private static boolean readAllowOverlap(MapEntityDefinition entity) {
        for (ComponentDefinition comp : entity.components()) {
            if (!"Submap".equals(comp.type().getSimpleName())) {
                continue;
            }
            Object value = comp.properties().get("allowOverlap");
            if (value instanceof Boolean b) {
                return b;
            }
            if (value instanceof String s) {
                return Boolean.parseBoolean(s);
            }
            return false;
        }
        return false;
    }
}
