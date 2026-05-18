package ca.kieve.ssss.context;

import ca.kieve.ssss.util.BoundingBox3i;
import ca.kieve.ssss.util.Vec3i;
import ca.kieve.ssss.world.MapRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks the set of {@link MapRegion}s that make up the loaded world,
 * with a per-cell ownership grid for fast region lookup.
 *
 * <p>Construction lifecycle: created empty as a {@link GameContext}
 * record component, then populated by the map generator —
 * {@link #init(Vec3i)} allocates the ownership grid once world
 * dimensions are known, {@link #addRegion} appends each loaded region,
 * and {@link #markOwnership} stamps each cell with its owning region.
 * After this, {@link #regionAt} answers in O(1).
 *
 * <p>Before {@code init}, {@link #regionAt} returns {@code null} — "no
 * map loaded yet" rather than throwing, so early/defensive lookups don't
 * have to special-case engine init order.
 */
public class MapContext {
    private final List<MapRegion> m_regions = new ArrayList<>();

    private MapRegion[][][] m_ownership;
    private BoundingBox3i m_box;

    /**
     * Allocates the per-cell ownership grid sized to {@code size}, with
     * its origin at {@code (0, 0, 0)}. Must be called before
     * {@link #markOwnership} and before any meaningful
     * {@link #regionAt} query.
     */
    public void init(Vec3i size) {
        m_box = new BoundingBox3i(Vec3i.ZERO, size);
        m_ownership = new MapRegion[size.x][size.y][size.z];
    }

    public void addRegion(MapRegion region) {
        m_regions.add(region);
    }

    public List<MapRegion> getRegions() {
        return Collections.unmodifiableList(m_regions);
    }

    /**
     * Stamps a single world-space cell as belonging to {@code region}.
     * Caller is responsible for any overlap policy; this method
     * unconditionally overwrites any existing stamp.
     *
     * @throws IllegalStateException if {@link #init} has not been called
     */
    public void markOwnership(Vec3i pos, MapRegion region) {
        if (m_ownership == null) {
            throw new IllegalStateException("MapContext.markOwnership requires init(size) first");
        }
        if (!m_box.contains(pos)) {
            throw new IndexOutOfBoundsException("markOwnership out of bounds: " + pos);
        }
        m_ownership[pos.x][pos.y][pos.z] = region;
    }

    /**
     * Returns the region that owns the given world-space cell, or
     * {@code null} if no region claims it (including the pre-init case).
     */
    public MapRegion regionAt(Vec3i worldPos) {
        if (m_ownership == null || !m_box.contains(worldPos)) {
            return null;
        }
        return m_ownership[worldPos.x][worldPos.y][worldPos.z];
    }
}
