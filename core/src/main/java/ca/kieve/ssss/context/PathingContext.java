package ca.kieve.ssss.context;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Solid;
import ca.kieve.ssss.util.Vec3i;
import com.github.yellowstonegames.grid.Coord;
import com.github.yellowstonegames.path.DijkstraMap;
import dev.dominion.ecs.api.Dominion;

import java.util.HashMap;
import java.util.Map;

/**
 * Caches dijkstra maps for pathfinding, avoiding repeated rebuilds per AI entity.
 * The map is rebuilt once per tick by PathingSystem.
 */
public class PathingContext {
    private static final int MAP_SIZE = 200;
    private static final char PASSABLE = '.';
    private static final char BLOCKED = '#';

    private Dominion m_ecs;

    // Per-Z-level dijkstra maps and grids
    private final Map<Integer, DijkstraMap> m_dijkstraMaps = new HashMap<>();
    private final Map<Integer, char[][]> m_grids = new HashMap<>();

    // Track which Z-levels need rebuilding
    private final Map<Integer, Boolean> m_dirty = new HashMap<>();

    public void init(GameContext gameContext) {
        m_ecs = gameContext.ecs();
    }

    /**
     * Marks all Z-levels as dirty, forcing a rebuild on next update.
     */
    public void markAllDirty() {
        for (var entry : m_dirty.entrySet()) {
            entry.setValue(true);
        }
    }

    /**
     * Marks a specific Z-level as dirty.
     */
    public void markDirty(int zLevel) {
        m_dirty.put(zLevel, true);
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
     */
    public Coord findNextStep(Vec3i start, Vec3i goal) {
        if (start.z != goal.z) {
            return null;
        }

        int zLevel = start.z;
        ensureMapExists(zLevel);

        DijkstraMap dijkstra = m_dijkstraMaps.get(zLevel);
        char[][] grid = m_grids.get(zLevel);

        // Ensure goal is passable for pathfinding
        boolean goalWasBlocked = false;
        if (goal.x >= 0 && goal.x < MAP_SIZE && goal.y >= 0 && goal.y < MAP_SIZE) {
            if (grid[goal.x][goal.y] == BLOCKED) {
                grid[goal.x][goal.y] = PASSABLE;
                goalWasBlocked = true;
            }
        }

        // Reinitialize if goal was blocked (temporary modification)
        if (goalWasBlocked) {
            dijkstra.initialize(grid);
        }

        Coord startCoord = Coord.get(start.x, start.y);
        Coord goalCoord = Coord.get(goal.x, goal.y);

        dijkstra.setGoal(goalCoord);
        dijkstra.scan(null);

        var path = dijkstra.findPath(1, null, null, startCoord, goalCoord);

        // Restore grid if we modified it
        if (goalWasBlocked) {
            grid[goal.x][goal.y] = BLOCKED;
            dijkstra.initialize(grid);
        }

        if (path.isEmpty()) {
            return null;
        }

        return path.first();
    }

    private void ensureMapExists(int zLevel) {
        if (!m_grids.containsKey(zLevel)) {
            char[][] grid = new char[MAP_SIZE][MAP_SIZE];
            for (int x = 0; x < MAP_SIZE; x++) {
                for (int y = 0; y < MAP_SIZE; y++) {
                    grid[x][y] = PASSABLE;
                }
            }
            m_grids.put(zLevel, grid);
            m_dijkstraMaps.put(zLevel, new DijkstraMap(grid));
            m_dirty.put(zLevel, true);
        }
    }

    private void rebuildGrid(int zLevel) {
        ensureMapExists(zLevel);

        char[][] grid = m_grids.get(zLevel);

        // Reset grid to passable
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                grid[x][y] = PASSABLE;
            }
        }

        // Mark solid entities as blocked
        var solids = m_ecs.findEntitiesWith(Solid.class, Position.class);
        solids.forEach(result -> {
            var pos = result.comp2().getPosition();
            if (pos.z == zLevel
                    && pos.x >= 0 && pos.x < MAP_SIZE
                    && pos.y >= 0 && pos.y < MAP_SIZE) {
                grid[pos.x][pos.y] = BLOCKED;
            }
        });

        m_dijkstraMaps.get(zLevel).initialize(grid);
    }
}
