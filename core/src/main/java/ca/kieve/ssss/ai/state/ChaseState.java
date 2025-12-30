package ca.kieve.ssss.ai.state;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Solid;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;
import com.github.yellowstonegames.grid.Coord;
import com.github.yellowstonegames.path.DijkstraMap;
import dev.dominion.ecs.api.Entity;

import java.util.Map;

/**
 * State that pathfinds toward the target.
 * Logic ported from AiChaserSystem.
 */
public class ChaseState extends AiState {
    private static final int MAP_SIZE = 200;

    private DijkstraMap m_dijkstraMap;
    private char[][] m_grid;

    @Override
    public void initialize(Map<String, Object> properties) {
        super.initialize(properties);
        m_grid = new char[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                m_grid[x][y] = '.';
            }
        }
        m_dijkstraMap = new DijkstraMap(m_grid);
    }

    @Override
    public void execute(StateContext context) {
        Entity target = context.targetEntity();
        if (target == null) {
            return;
        }

        var targetPosComp = target.get(Position.class);
        if (targetPosComp == null) {
            return;
        }

        var entityPosComp = context.entity().get(Position.class);
        var velocity = context.entity().get(Velocity.class);
        if (entityPosComp == null || velocity == null) {
            return;
        }

        Vec3i pos = entityPosComp.getPosition();
        Vec3i targetPos = targetPosComp.getPosition();

        // Only chase on same Z level
        if (pos.z != targetPos.z) {
            return;
        }

        // Stop if adjacent to target
        int dist = Math.abs(pos.x - targetPos.x) + Math.abs(pos.y - targetPos.y);
        if (dist <= 1) {
            return;
        }

        // Update grid with solids
        updateGrid(context.gameContext(), targetPos);

        // Pathfind
        Coord start = Coord.get(pos.x, pos.y);
        Coord goal = Coord.get(targetPos.x, targetPos.y);
        m_dijkstraMap.setGoal(goal);
        m_dijkstraMap.scan(null);

        var path = m_dijkstraMap.findPath(1, null, null, start, goal);
        if (path.isEmpty()) {
            return;
        }

        Coord next = path.get(0);
        int dx = next.x - pos.x;
        int dy = next.y - pos.y;

        if (dx != 0 || dy != 0) {
            velocity.instant().set(new Vec3i(dx, dy, 0));
        }
    }

    private void updateGrid(GameContext gameContext, Vec3i targetPos) {
        // Reset grid
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                m_grid[x][y] = '.';
            }
        }

        // Mark solids
        var solids = gameContext.ecs().findEntitiesWith(Solid.class, Position.class);
        solids.forEach(result -> {
            var pos = result.comp2().getPosition();
            if (pos.z == targetPos.z
                    && pos.x >= 0 && pos.x < MAP_SIZE
                    && pos.y >= 0 && pos.y < MAP_SIZE) {
                m_grid[pos.x][pos.y] = '#';
            }
        });

        // Ensure target pos is passable
        if (targetPos.x >= 0 && targetPos.x < MAP_SIZE
                && targetPos.y >= 0 && targetPos.y < MAP_SIZE) {
            m_grid[targetPos.x][targetPos.y] = '.';
        }

        m_dijkstraMap.initialize(m_grid);
    }
}
