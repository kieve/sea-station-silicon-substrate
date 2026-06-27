package ca.kieve.ssss.content.map;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.MapContext;
import ca.kieve.ssss.context.WorldContext;
import ca.kieve.ssss.util.Vec3i;
import ca.kieve.ssss.world.MapRegion;
import ca.kieve.ssss.world.WorldModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a map YAML (leaf or composition) into a {@link WorldModel}.
 *
 * <p>For a leaf YAML this is a single-region load: the file's
 * {@code layers} are written into the world at offset {@code (0,0,0)}
 * and the corresponding {@link MapRegion} is registered in
 * {@link MapContext}.
 *
 * <p>For a composition YAML, {@link CompositeMapLoader} walks the submap
 * tree and resolves each leaf's world-space offset; this class then
 * sizes the {@link WorldModel} to the union, stamps cells with overlap
 * detection, and translates child entity positions to world coordinates.
 */
public class YamlMapGenerator implements MapGenerator {
    private final String m_rootMapPath;
    private final CompositeMapLoader m_loader;

    private String m_floorGlyph;
    private List<MapEntityDefinition> m_translatedEntities = List.of();

    public YamlMapGenerator(String rootMapPath) {
        m_rootMapPath = rootMapPath;
        m_loader = new CompositeMapLoader();
    }

    @Override
    public void generate(
        BlockTypeFactory blockTypeFactory,
        MapContext mapContext,
        WorldContext worldContext,
        FluidContext fluidContext
    ) {
        List<CompositeMapLoader.Placement> placements = m_loader.load(m_rootMapPath);
        m_floorGlyph = placements.get(0).definition().floorGlyph();

        Vec3i worldBounds = computeWorldBounds(placements);
        if (worldBounds.x == 0 || worldBounds.y == 0 || worldBounds.z == 0) {
            throw new IllegalStateException(
                "composition resolved to empty world bounds; "
                    + "at least one placement must contribute layers"
            );
        }
        WorldModel world = new WorldModel(
            worldBounds.x,
            worldBounds.y,
            worldBounds.z,
            blockTypeFactory
        );
        mapContext.init(worldBounds);
        fluidContext.init(worldBounds);

        Map<String, MapRegion> regionsById = new HashMap<>();
        for (CompositeMapLoader.Placement p : placements) {
            if (isEmptyBounds(p.localBounds())) {
                continue;
            }
            MapRegion region = new MapRegion(p.id(), p.worldOffset(), p.localBounds());
            regionsById.put(p.id(), region);
            mapContext.addRegion(region);
        }

        for (CompositeMapLoader.Placement p : placements) {
            MapRegion region = regionsById.get(p.id());
            if (region == null) {
                continue;
            }
            stampPlacement(p, region, world, mapContext, fluidContext);
        }

        m_translatedEntities = collectTranslatedEntities(placements);
        worldContext.setModel(world);
    }

    @Override
    public String getFloorGlyphId() {
        return m_floorGlyph;
    }

    @Override
    public List<MapEntityDefinition> getEntities() {
        return m_translatedEntities;
    }

    private static Vec3i computeWorldBounds(List<CompositeMapLoader.Placement> placements) {
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        for (CompositeMapLoader.Placement p : placements) {
            if (isEmptyBounds(p.localBounds())) {
                continue;
            }
            maxX = Math.max(maxX, p.worldOffset().x + p.localBounds().x);
            maxY = Math.max(maxY, p.worldOffset().y + p.localBounds().y);
            maxZ = Math.max(maxZ, p.worldOffset().z + p.localBounds().z);
        }
        return new Vec3i(maxX, maxY, maxZ);
    }

    private static boolean isEmptyBounds(Vec3i bounds) {
        return bounds.x == 0 || bounds.y == 0 || bounds.z == 0;
    }

    private static void stampPlacement(
        CompositeMapLoader.Placement placement,
        MapRegion region,
        WorldModel world,
        MapContext mapContext,
        FluidContext fluidContext
    ) {
        Map<Character, MapBlockDefinition> charMap = buildCharMap(placement.definition().blocks());
        Vec3i offset = placement.worldOffset();

        for (Map.Entry<String, String> layerEntry : placement.definition().layers().entrySet()) {
            int localZ = Integer.parseInt(layerEntry.getKey());
            String[] lines = layerEntry.getValue().split("\n");
            for (int localY = 0; localY < lines.length; localY++) {
                String line = lines[localY];
                for (int localX = 0; localX < line.length(); localX++) {
                    MapBlockDefinition blockDef = charMap.get(line.charAt(localX));
                    if (blockDef == null) {
                        continue;
                    }
                    Vec3i worldCell = offset.add(new Vec3i(localX, localY, localZ));
                    MapRegion existing = mapContext.regionAt(worldCell);
                    if (existing != null && existing != region) {
                        if (!placement.allowOverlap()) {
                            throw new IllegalStateException(
                                "overlap conflict at " + worldCell + ": regions '"
                                    + existing.id() + "' and '" + region.id()
                                    + "' both claim this cell"
                            );
                        }
                    }
                    if (blockDef.bpId() != null) {
                        world.setBlock(worldCell, blockDef.bpId());
                    }
                    if (blockDef.waterFill() != null) {
                        fluidContext.setMass(worldCell, blockDef.waterFill());
                    }
                    if (blockDef.waterDepth() != null) {
                        fluidContext.setSource(worldCell, blockDef.waterDepth());
                    }
                    mapContext.markOwnership(worldCell, region);
                }
            }
        }
    }

    private static Map<Character, MapBlockDefinition> buildCharMap(
        Map<String, MapBlockDefinition> blocks
    ) {
        Map<Character, MapBlockDefinition> result = new HashMap<>();
        for (MapBlockDefinition blockDef : blocks.values()) {
            result.put(blockDef.layoutChar(), blockDef);
        }
        return result;
    }

    private static List<MapEntityDefinition> collectTranslatedEntities(
        List<CompositeMapLoader.Placement> placements
    ) {
        List<MapEntityDefinition> out = new ArrayList<>();
        for (CompositeMapLoader.Placement p : placements) {
            Vec3i offset = p.worldOffset();
            for (MapEntityDefinition entity : p.definition().entities()) {
                out.add(translateEntity(entity, offset));
            }
        }
        return out;
    }

    private static MapEntityDefinition translateEntity(MapEntityDefinition entity, Vec3i offset) {
        if (offset.x == 0 && offset.y == 0 && offset.z == 0) {
            return entity;
        }
        List<ComponentDefinition> translatedComponents = new ArrayList<>();
        for (ComponentDefinition comp : entity.components()) {
            if (comp.type() == Position.class) {
                translatedComponents.add(translatePositionComponent(comp, offset));
            } else {
                translatedComponents.add(comp);
            }
        }
        return new MapEntityDefinition(entity.id(), translatedComponents);
    }

    private static ComponentDefinition translatePositionComponent(
        ComponentDefinition source,
        Vec3i offset
    ) {
        ComponentDefinition translated = new ComponentDefinition(source.type());
        for (Map.Entry<String, Object> e : source.properties().entrySet()) {
            translated.setProperty(e.getKey(), e.getValue());
        }
        translated.setProperty("x", asInt(source.properties().get("x")) + offset.x);
        translated.setProperty("y", asInt(source.properties().get("y")) + offset.y);
        translated.setProperty("z", asInt(source.properties().get("z")) + offset.z);
        return translated;
    }

    private static int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            return Integer.parseInt(s);
        }
        throw new IllegalArgumentException(
            "expected numeric position component, got " + value.getClass()
        );
    }
}
