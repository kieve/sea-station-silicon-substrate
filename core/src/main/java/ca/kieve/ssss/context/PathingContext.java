package ca.kieve.ssss.context;

import com.github.yellowstonegames.grid.Coord;
import com.github.yellowstonegames.path.DijkstraMap;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.MaxPassableSize;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Size;
import ca.kieve.ssss.util.PerfClock;
import ca.kieve.ssss.util.SolidUtil;
import ca.kieve.ssss.util.Vec3i;
import ca.kieve.ssss.world.MapRegion;
import ca.kieve.ssss.world.Portal;
import ca.kieve.ssss.world.RegionGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Caches DijkstraMaps for pathfinding, keyed by {@code (regionId, zLevel)}
 * (and additionally by mover {@link Size} for size-aware queries). Each
 * region's grid is sized to the region's own footprint — not to the whole
 * world — so composing more regions doesn't grow individual grids and a
 * dirty entity in one region doesn't invalidate anything in another.
 *
 * <p>Cross-region paths route via a {@link RegionGraph}: a coarse BFS
 * over regions picks the next portal to head toward, then the
 * within-region DijkstraMap routes to that portal's cell. Returns one
 * world-space next-step per query, same shape as same-region paths.
 */
public class PathingContext {
    private record RegionLevelKey(String regionId, int zLevel) {
    }

    private record SizedRegionLevelKey(String regionId, int zLevel, Size size) {
    }

    /**
     * Per-region pathing state. Storage stays {@code char[][]} so it can
     * be handed to {@link DijkstraMap} without copying, but the API
     * speaks booleans so PathingContext doesn't have to reason about
     * SquidSquad's wall/floor character convention.
     */
    private static final class RegionGrid {
        private static final char OPEN = '.';
        private static final char BLOCKED = '#';

        final int zLevel;
        final int originX;
        final int originY;
        final int width;
        final int height;
        final DijkstraMap dijkstra;

        private final char[][] m_grid;

        Coord lastGoal;
        boolean dirty = true;

        RegionGrid(MapRegion region, int zLevel) {
            this.zLevel = zLevel;
            this.originX = region.box().origin().x;
            this.originY = region.box().origin().y;
            this.width = region.box().size().x;
            this.height = region.box().size().y;
            this.m_grid = new char[width][height];
            clearAll();
            this.dijkstra = new DijkstraMap(m_grid);
        }

        boolean isBlocked(int localX, int localY) {
            return m_grid[localX][localY] == BLOCKED;
        }

        void setBlocked(int localX, int localY, boolean blocked) {
            m_grid[localX][localY] = blocked ? BLOCKED : OPEN;
        }

        void clearAll() {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    m_grid[x][y] = OPEN;
                }
            }
        }

        /**
         * Re-initialises the DijkstraMap from the current grid. Call
         * after mutating cell state via {@link #setBlocked} or
         * {@link #clearAll} before issuing pathing queries.
         */
        void refreshDijkstra() {
            dijkstra.initialize(m_grid);
        }
    }

    private final Map<RegionLevelKey, RegionGrid> m_grids = new HashMap<>();
    private final Map<SizedRegionLevelKey, RegionGrid> m_sizedGrids = new HashMap<>();

    private Dominion m_ecs;
    private PositionContext m_pos;
    private MapContext m_map;
    private PerfClock m_perf;
    private RegionGraph m_regionGraph;

    public void init(GameContext gameContext) {
        m_ecs = gameContext.ecs();
        m_pos = gameContext.pos();
        m_map = gameContext.map();
        m_perf = gameContext.perf();
        m_regionGraph = RegionGraph.discover(gameContext.world().getModel(), m_map);
    }

    public RegionGraph getRegionGraph() {
        return m_regionGraph;
    }

    /**
     * Marks every cached grid (sized and unsized) as dirty. Called by
     * {@link ca.kieve.ssss.system.PathingSystem#preTick} since any entity
     * may have moved between turns.
     */
    public void markAllDirty() {
        for (var grid : m_grids.values()) {
            grid.dirty = true;
            grid.lastGoal = null;
        }
        for (var grid : m_sizedGrids.values()) {
            grid.dirty = true;
            grid.lastGoal = null;
        }
    }

    /**
     * Marks a single region's grid at the given z-level as dirty.
     */
    public void markDirty(String regionId, int zLevel) {
        var key = new RegionLevelKey(regionId, zLevel);
        var grid = m_grids.get(key);
        if (grid != null) {
            grid.dirty = true;
            grid.lastGoal = null;
        }
        // Sized grids for this region/z too.
        for (var entry : m_sizedGrids.entrySet()) {
            if (entry.getKey().regionId().equals(regionId) && entry.getKey().zLevel() == zLevel) {
                entry.getValue().dirty = true;
                entry.getValue().lastGoal = null;
            }
        }
    }

    /**
     * Finds the next step from {@code start} toward {@code goal} for a
     * region-aware path. Returns world-space coordinates of the step, or
     * {@code null} if there is no path (including the cross-region case
     * until Phase 4b lands).
     */
    public Coord findNextStep(Vec3i start, Vec3i goal) {
        m_perf.start("Pathing-findNextStep");
        try {
            return findNextStepInternal(start, goal, null);
        } finally {
            m_perf.end("Pathing-findNextStep");
        }
    }

    /**
     * Size-aware variant of {@link #findNextStep(Vec3i, Vec3i)}. The grid
     * additionally blocks {@link MaxPassableSize}-restricted cells that
     * the mover is too large to traverse.
     */
    public Coord findNextStep(Vec3i start, Vec3i goal, Entity mover) {
        m_perf.start("Pathing-findNextStepSized");
        try {
            return findNextStepInternal(start, goal, mover);
        } finally {
            m_perf.end("Pathing-findNextStepSized");
        }
    }

    private Coord findNextStepInternal(Vec3i start, Vec3i goal, Entity mover) {
        if (start.z != goal.z) {
            return null;
        }
        MapRegion startRegion = m_map.regionAt(start);
        if (startRegion == null) {
            return null;
        }
        MapRegion goalRegion = m_map.regionAt(goal);
        if (goalRegion == null) {
            return null;
        }
        if (!goalRegion.id().equals(startRegion.id())) {
            return crossRegionNextStep(start, goal, mover, startRegion, goalRegion);
        }
        return sameRegionNextStep(start, goal, mover, startRegion);
    }

    private Coord crossRegionNextStep(
        Vec3i start,
        Vec3i goal,
        Entity mover,
        MapRegion startRegion,
        MapRegion goalRegion
    ) {
        Predicate<Portal> traversable = portal -> isPortalTraversable(portal, mover);
        Portal portal = m_regionGraph.findPortalTowards(
            startRegion.id(),
            goalRegion.id(),
            start,
            traversable
        );
        if (portal == null) {
            return null;
        }
        Vec3i exitCell = portal.cellIn(startRegion.id());
        Vec3i entryCell = portal.cellIn(portal.otherRegion(startRegion.id()));
        // Already on the portal cell — step across into the next region.
        if (start.equals(exitCell)) {
            return Coord.get(entryCell.x, entryCell.y);
        }
        // Otherwise, route within startRegion toward the exit cell.
        return sameRegionNextStep(start, exitCell, mover, startRegion);
    }

    private boolean isPortalTraversable(Portal portal, Entity mover) {
        if (mover == null) {
            return !SolidUtil.hasSolid(m_pos, portal.cellInA())
                && !SolidUtil.hasSolid(m_pos, portal.cellInB());
        }
        return !SolidUtil.isBlockedFor(m_pos, portal.cellInA(), mover)
            && !SolidUtil.isBlockedFor(m_pos, portal.cellInB(), mover);
    }

    private Coord sameRegionNextStep(Vec3i start, Vec3i goal, Entity mover, MapRegion region) {
        RegionGrid rg = (mover == null)
            ? getOrCreateGrid(region, start.z)
            : getOrCreateSizedGrid(region, start.z, SolidUtil.getSize(mover));

        if (rg.dirty) {
            rebuildGrid(rg, (mover == null) ? null : SolidUtil.getSize(mover));
            rg.dirty = false;
        }

        int localStartX = start.x - rg.originX;
        int localStartY = start.y - rg.originY;
        int localGoalX = goal.x - rg.originX;
        int localGoalY = goal.y - rg.originY;

        // Both should be in-bounds since both were inside the region's box.
        if (!inBounds(rg, localStartX, localStartY) || !inBounds(rg, localGoalX, localGoalY)) {
            return null;
        }

        // The goal cell can be blocked (e.g., target entity has Solid).
        // Temporarily unblock so the path can terminate on it, then restore.
        boolean goalWasBlocked = rg.isBlocked(localGoalX, localGoalY);
        if (goalWasBlocked) {
            rg.setBlocked(localGoalX, localGoalY, false);
            rg.refreshDijkstra();
            rg.lastGoal = null;
        }

        Coord goalCoord = Coord.get(localGoalX, localGoalY);
        if (rg.lastGoal == null || !rg.lastGoal.equals(goalCoord)) {
            rg.dijkstra.setGoal(goalCoord);
            rg.dijkstra.scan(null);
            rg.lastGoal = goalCoord;
        }

        Coord startCoord = Coord.get(localStartX, localStartY);
        var path = rg.dijkstra.findPath(1, null, null, startCoord, goalCoord);

        if (goalWasBlocked) {
            rg.setBlocked(localGoalX, localGoalY, true);
            rg.refreshDijkstra();
            rg.lastGoal = null;
        }

        if (path.isEmpty()) {
            return null;
        }
        Coord local = path.first();
        return Coord.get(local.x + rg.originX, local.y + rg.originY);
    }

    private RegionGrid getOrCreateGrid(MapRegion region, int zLevel) {
        var key = new RegionLevelKey(region.id(), zLevel);
        return m_grids.computeIfAbsent(key, k -> new RegionGrid(region, zLevel));
    }

    private RegionGrid getOrCreateSizedGrid(MapRegion region, int zLevel, Size size) {
        var key = new SizedRegionLevelKey(region.id(), zLevel, size);
        return m_sizedGrids.computeIfAbsent(key, k -> new RegionGrid(region, zLevel));
    }

    private void rebuildGrid(RegionGrid rg, Size moverSize) {
        m_perf.start("Pathing-rebuildGrid");
        try {
            rg.clearAll();

            // Single sweep over positioned entities. The blocking predicate
            // lives in SolidUtil.blocksMover so Openable/MaxPassableSize/
            // future condition rules apply consistently here and at the
            // per-cell traversability checks (cross-region portals).
            m_ecs.findEntitiesWith(Position.class).forEach(result -> {
                var pos = result.comp().getPosition();
                if (pos.z != rg.zLevel) {
                    return;
                }
                int lx = pos.x - rg.originX;
                int ly = pos.y - rg.originY;
                if (!inBounds(rg, lx, ly)) {
                    return;
                }
                if (SolidUtil.blocksMover(result.entity(), moverSize)) {
                    rg.setBlocked(lx, ly, true);
                }
            });

            rg.refreshDijkstra();
        } finally {
            m_perf.end("Pathing-rebuildGrid");
        }
    }

    private static boolean inBounds(RegionGrid rg, int lx, int ly) {
        return lx >= 0 && lx < rg.width && ly >= 0 && ly < rg.height;
    }
}
