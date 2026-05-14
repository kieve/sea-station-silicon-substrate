package ca.kieve.ssss.context;

import com.github.yellowstonegames.grid.FOV;

import ca.kieve.ssss.util.OpaqueGrid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores field-of-view state, explored tiles, and ghost data for fog of war.
 * Owns no ECS reference — VisionSystem supplies the opaque grid each recalculation.
 */
public class VisionContext {
    private static final int MAP_WIDTH = 28;
    private static final int MAP_HEIGHT = 17;

    private final float[][] m_resistance = new float[MAP_WIDTH][MAP_HEIGHT];
    private final float[][] m_visibility = new float[MAP_WIDTH][MAP_HEIGHT];

    // TODO: m_ghostsByZ and m_exploredByZ grow unbounded as the player explores
    // — every newly-seen tile sticks around forever. Fine for any reasonable
    // single-session map; revisit if memory/perf ever shows up as a problem.
    private final Map<Integer, Map<String, GhostTile>> m_ghostsByZ = new HashMap<>();
    private final Map<Integer, Set<String>> m_exploredByZ = new HashMap<>();

    private int m_cameraZ = 1;

    public void recalculate(int playerX, int playerY, int cameraZ, OpaqueGrid opaqueGrid) {
        m_cameraZ = cameraZ;

        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                m_resistance[x][y] = opaqueGrid.isOpaque(x, y) ? 1f : 0f;
                m_visibility[x][y] = 0f;
            }
        }

        if (playerX < 0 || playerX >= MAP_WIDTH
            || playerY < 0 || playerY >= MAP_HEIGHT) {
            return;
        }

        int radius = Math.max(MAP_WIDTH, MAP_HEIGHT);
        FOV.reuseFOV(m_resistance, m_visibility, playerX, playerY, radius);

        var explored = m_exploredByZ.computeIfAbsent(cameraZ, k -> new HashSet<>());
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (m_visibility[x][y] > 0) {
                    explored.add(x + "," + y);
                }
            }
        }
    }

    public int getCameraZ() {
        return m_cameraZ;
    }

    public boolean isVisible(int x, int y) {
        if (x < 0 || x >= MAP_WIDTH || y < 0 || y >= MAP_HEIGHT) {
            return false;
        }
        return m_visibility[x][y] > 0;
    }

    public boolean isExplored(int x, int y, int cameraZ) {
        var explored = m_exploredByZ.get(cameraZ);
        return explored != null && explored.contains(x + "," + y);
    }

    public void setGhost(int x, int y, int cameraZ, GhostTile ghost) {
        var ghosts = m_ghostsByZ.computeIfAbsent(cameraZ, k -> new HashMap<>());
        ghosts.put(x + "," + y, ghost);
    }

    public GhostTile getGhost(int x, int y, int cameraZ) {
        var ghosts = m_ghostsByZ.get(cameraZ);
        if (ghosts == null) {
            return null;
        }
        return ghosts.get(x + "," + y);
    }

    public Map<String, GhostTile> getGhostEntries(int cameraZ) {
        return m_ghostsByZ.getOrDefault(cameraZ, Map.of());
    }
}
