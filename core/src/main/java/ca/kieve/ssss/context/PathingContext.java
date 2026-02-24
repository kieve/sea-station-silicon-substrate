package ca.kieve.ssss.context;

import java.util.HashMap;
import java.util.Map;

import com.github.yellowstonegames.grid.Coord;
import com.github.yellowstonegames.path.DijkstraMap;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.MaxPassableSize;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Size;
import ca.kieve.ssss.component.Solid;
import ca.kieve.ssss.util.PerfClock;
import ca.kieve.ssss.util.SolidUtil;
import ca.kieve.ssss.util.Vec3i;

/**
 * Caches dijkstra maps for pathfinding, avoiding repeated rebuilds per AI entity.
 * The map is rebuilt once per tick by PathingSystem.
 */
public class PathingContext {
    private static final int MAP_WIDTH = 28;
    private static final int MAP_HEIGHT = 17;
    private static final char PASSABLE = '.';
    private static final char BLOCKED = '#';

    private record SizedLevel(int zLevel, Size size) {}

    private Dominion m_ecs;
    private PositionContext m_positionContext;
    private PerfClock m_perf;

    // Per-Z-level dijkstra maps and grids (for non-size-aware queries)
    private final Map<Integer, DijkstraMap> m_dijkstraMaps = new HashMap<>();
    private final Map<Integer, char[][]> m_grids = new HashMap<>();

    // Per-(Z-level, Size) dijkstra maps and grids for size-aware pathfinding
    private final Map<SizedLevel, DijkstraMap> m_sizedDijkstraMaps = new HashMap<>();
    private final Map<SizedLevel, char[][]> m_sizedGrids = new HashMap<>();

    // Track which Z-levels need rebuilding
    private final Map<Integer, Boolean> m_dirty = new HashMap<>();

    // Track the last scanned goal per Z-level for caching
    private final Map<Integer, Coord> m_lastGoal = new HashMap<>();
    private final Map<SizedLevel, Coord> m_sizedLastGoal = new HashMap<>();

    public void init(GameContext gameContext) {
        m_ecs = gameContext.ecs();
        m_positionContext = gameContext.pos();
        m_perf = gameContext.perf();
    }

    /**
     * Marks all Z-levels as dirty, forcing a rebuild on next update.
     * Also clears the goal cache since entity positions may have changed.
     */
    public void markAllDirty() {
        for (var entry : m_dirty.entrySet()) {
            entry.setValue(true);
        }
        m_lastGoal.clear();
        // Clear sized caches - they will be rebuilt on demand
        m_sizedDijkstraMaps.clear();
        m_sizedGrids.clear();
        m_sizedLastGoal.clear();
    }

    /**
     * Marks a specific Z-level as dirty.
     * Also clears the goal cache for that level.
     */
    public void markDirty(int zLevel) {
        m_dirty.put(zLevel, true);
        m_lastGoal.remove(zLevel);
    }

    /**
     * Updates the pathfinding grid for a Z-level if dirty.
     * Called by PathingSystem each tick.
     */
    public void updateIfDirty(int zLevel) {
        Boolean dirty = m_dirty.get(zLevel);
        if (dirty == null || dirty) {
            rebuildGrid(zLevel);
            m_dirty.put(zLevel, false);
        }
    }

    /**
     * Finds a path from start to goal on the same Z-level.
     * Returns the next step coordinate, or null if no path exists.
     *
     * Uses caching: if the goal hasn't changed since the last scan,
     * the expensive scan() is skipped and only findPath() is called.
     */
    public Coord findNextStep(Vec3i start, Vec3i goal) {
        m_perf.start("Pathing-findNextStep");
        try {
            return findNextStepInternal(start, goal);
        } finally {
            m_perf.end("Pathing-findNextStep");
        }
    }

    private Coord findNextStepInternal(Vec3i start, Vec3i goal) {
        if (start.z != goal.z) {
            return null;
        }

        int zLevel = start.z;
        ensureMapExists(zLevel);

        // Rebuild grid if dirty (entity positions changed)
        updateIfDirty(zLevel);

        DijkstraMap dijkstra = m_dijkstraMaps.get(zLevel);
        char[][] grid = m_grids.get(zLevel);

        Coord goalCoord = Coord.get(goal.x, goal.y);

        // Check if goal is blocked and needs temporary unblocking
        boolean goalWasBlocked = false;
        if (goal.x >= 0 && goal.x < MAP_WIDTH && goal.y >= 0 && goal.y < MAP_HEIGHT) {
            if (grid[goal.x][goal.y] == BLOCKED) {
                grid[goal.x][goal.y] = PASSABLE;
                goalWasBlocked = true;
                dijkstra.initialize(grid);
                // Invalidate cache since we modified the grid
                m_lastGoal.remove(zLevel);
            }
        }

        // Only scan if goal changed or cache is empty for this level
        Coord cachedGoal = m_lastGoal.get(zLevel);
        if (cachedGoal == null || !cachedGoal.equals(goalCoord)) {
            dijkstra.setGoal(goalCoord);
            dijkstra.scan(null);
            m_lastGoal.put(zLevel, goalCoord);
        }

        Coord startCoord = Coord.get(start.x, start.y);
        var path = dijkstra.findPath(1, null, null, startCoord, goalCoord);

        // Restore grid if we modified it
        if (goalWasBlocked) {
            grid[goal.x][goal.y] = BLOCKED;
            dijkstra.initialize(grid);
            // Invalidate cache since grid is restored
            m_lastGoal.remove(zLevel);
        }

        if (path.isEmpty()) {
            return null;
        }

        return path.first();
    }

    /**
     * Finds a path from start to goal for a specific entity.
     * Takes into account the entity's size for passability through size-restricted passages.
     */
    public Coord findNextStep(Vec3i start, Vec3i goal, Entity mover) {
        m_perf.start("Pathing-findNextStepSized");
        try {
            return findNextStepSizedInternal(start, goal, mover);
        } finally {
            m_perf.end("Pathing-findNextStepSized");
        }
    }

    private Coord findNextStepSizedInternal(
            Vec3i start,
            Vec3i goal,
            Entity mover) {
        if (start.z != goal.z) {
            return null;
        }

        int zLevel = start.z;
        Size moverSize = SolidUtil.getSize(mover);
        SizedLevel key = new SizedLevel(zLevel, moverSize);

        ensureSizedMapExists(key);

        DijkstraMap dijkstra = m_sizedDijkstraMaps.get(key);
        char[][] grid = m_sizedGrids.get(key);

        Coord goalCoord = Coord.get(goal.x, goal.y);

        // Check if goal is blocked and needs temporary unblocking
        boolean goalWasBlocked = false;
        if (goal.x >= 0 && goal.x < MAP_WIDTH
                && goal.y >= 0 && goal.y < MAP_HEIGHT) {
            if (grid[goal.x][goal.y] == BLOCKED) {
                grid[goal.x][goal.y] = PASSABLE;
                goalWasBlocked = true;
                dijkstra.initialize(grid);
                m_sizedLastGoal.remove(key);
            }
        }

        // Only scan if goal changed or cache is empty for this key
        Coord cachedGoal = m_sizedLastGoal.get(key);
        if (cachedGoal == null || !cachedGoal.equals(goalCoord)) {
            dijkstra.setGoal(goalCoord);
            dijkstra.scan(null);
            m_sizedLastGoal.put(key, goalCoord);
        }

        Coord startCoord = Coord.get(start.x, start.y);
        var path = dijkstra.findPath(
            1, null, null, startCoord, goalCoord);

        // Restore grid if we modified it
        if (goalWasBlocked) {
            grid[goal.x][goal.y] = BLOCKED;
            dijkstra.initialize(grid);
            m_sizedLastGoal.remove(key);
        }

        if (path.isEmpty()) {
            return null;
        }

        return path.first();
    }

    private void ensureSizedMapExists(SizedLevel key) {
        if (!m_sizedGrids.containsKey(key)) {
            char[][] grid = new char[MAP_WIDTH][MAP_HEIGHT];
            rebuildSizedGrid(key, grid);
            m_sizedGrids.put(key, grid);
            m_sizedDijkstraMaps.put(key, new DijkstraMap(grid));
        }
    }

    private void rebuildSizedGrid(SizedLevel key, char[][] grid) {
        m_perf.start("Pathing-rebuildSizedGrid");
        int zLevel = key.zLevel();
        Size size = key.size();

        // Reset grid to passable
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                grid[x][y] = PASSABLE;
            }
        }

        // Mark solid entities as blocked
        var solids = m_ecs.findEntitiesWith(Solid.class, Position.class);
        solids.forEach(result -> {
            var pos = result.comp2().getPosition();
            if (pos.z == zLevel
                    && pos.x >= 0 && pos.x < MAP_WIDTH
                    && pos.y >= 0 && pos.y < MAP_HEIGHT) {
                grid[pos.x][pos.y] = BLOCKED;
            }
        });

        // Mark size-restricted passages as blocked if entity is too large
        var restricted = m_ecs.findEntitiesWith(
            MaxPassableSize.class, Position.class);
        restricted.forEach(result -> {
            MaxPassableSize restriction = result.comp1();
            var pos = result.comp2().getPosition();
            if (pos.z == zLevel
                    && pos.x >= 0 && pos.x < MAP_WIDTH
                    && pos.y >= 0 && pos.y < MAP_HEIGHT
                    && !SolidUtil.canSizePassThrough(
                        size, restriction.maxSize())) {
                grid[pos.x][pos.y] = BLOCKED;
            }
        });
        m_perf.end("Pathing-rebuildSizedGrid");
    }

    private void ensureMapExists(int zLevel) {
        if (!m_grids.containsKey(zLevel)) {
            char[][] grid = new char[MAP_WIDTH][MAP_HEIGHT];
            for (int x = 0; x < MAP_WIDTH; x++) {
                for (int y = 0; y < MAP_HEIGHT; y++) {
                    grid[x][y] = PASSABLE;
                }
            }
            m_grids.put(zLevel, grid);
            m_dijkstraMaps.put(zLevel, new DijkstraMap(grid));
            m_dirty.put(zLevel, true);
        }
    }

    private void rebuildGrid(int zLevel) {
        m_perf.start("Pathing-rebuildGrid");
        ensureMapExists(zLevel);

        char[][] grid = m_grids.get(zLevel);

        // Reset grid to passable
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                grid[x][y] = PASSABLE;
            }
        }

        // Mark solid entities as blocked
        var solids = m_ecs.findEntitiesWith(Solid.class, Position.class);
        solids.forEach(result -> {
            var pos = result.comp2().getPosition();
            if (pos.z == zLevel
                    && pos.x >= 0 && pos.x < MAP_WIDTH
                    && pos.y >= 0 && pos.y < MAP_HEIGHT) {
                grid[pos.x][pos.y] = BLOCKED;
            }
        });

        m_dijkstraMaps.get(zLevel).initialize(grid);
        m_perf.end("Pathing-rebuildGrid");
    }
}
