package ca.kieve.ssss.system;

import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Solid;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.component.ai.AiChaser;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;
import com.github.yellowstonegames.grid.Coord;
import com.github.yellowstonegames.path.DijkstraMap;
import dev.dominion.ecs.api.Entity;

public class AiChaserSystem extends System {
    private DijkstraMap m_dijkstraMap;
    private char[][] m_grid;
    private static final int MAP_SIZE = 200;

    public AiChaserSystem(GameContext gameContext) {
        super(gameContext);
        m_grid = new char[MAP_SIZE][MAP_SIZE];
        // Initialize grid with floor
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                m_grid[x][y] = '.';
            }
        }
        m_dijkstraMap = new DijkstraMap(m_grid);
    }

    @Override
    public void tick() {
        // Find player to chase
        Entity player = findPlayer();
        if (player == null) {
            return;
        }
        var playerPosComp = player.get(Position.class);
        if (playerPosComp == null) {
            return;
        }
        Vec3i playerPos = playerPosComp.getPosition();

        // Check if any chasers can act before doing expensive map work
        var chasers = m_gameContext.ecs().findEntitiesWith(AiChaser.class, Position.class, Velocity.class, Speed.class);

        // 1. Update grid based on Solid entities
        // Reset grid to '.' 
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                m_grid[x][y] = '.';
            }
        }

        // Mark solids
        var solids = m_gameContext.ecs().findEntitiesWith(Solid.class, Position.class);
        solids.forEach(result -> {
            var pos = result.comp2().getPosition();
            if (pos.z == playerPos.z && pos.x >= 0 && pos.x < MAP_SIZE && pos.y >= 0 && pos.y < MAP_SIZE) {
                m_grid[pos.x][pos.y] = '#';
            }
        });
        
        // Ensure player pos is passable so we can path to it
        // (Player is likely Solid, so it was marked # above)
        if (playerPos.x >= 0 && playerPos.x < MAP_SIZE && playerPos.y >= 0 && playerPos.y < MAP_SIZE) {
            m_grid[playerPos.x][playerPos.y] = '.';
        }

        m_dijkstraMap.initialize(m_grid);
        
        // 2. Update DijkstraMap with player as target
        Coord target = Coord.get(playerPos.x, playerPos.y);
        m_dijkstraMap.setGoal(target);
        // Scan map. null means full scan.
        m_dijkstraMap.scan(null);

        // 3. Move chasers
        chasers.forEach(result -> {
            var chaser = result.comp1();
            var pos = result.comp2().getPosition();
            var velocity = result.comp3();
            var speed = result.comp4();

            if (!speed.canAct) {
                return;
            }
            
            if (pos.z != playerPos.z) {
                return; // Different floor
            }

            int dist = Math.abs(pos.x - playerPos.x) + Math.abs(pos.y - playerPos.y);
            if (chaser.range > 0 && dist > chaser.range) {
                return;
            }

            // Stop if adjacent to player to avoid overlapping
            if (dist <= 1) {
                return;
            }

            Coord start = Coord.get(pos.x, pos.y);
            
            // Find next step
            // We want a path of length 1 (just the next step)
            var path = m_dijkstraMap.findPath(1, null, null, start, target);
            if (path.isEmpty()) {
                return;
            }
            
            Coord next = path.get(0);
            
            // Determine direction
            int dx = next.x - pos.x;
            int dy = next.y - pos.y;
            
            if (dx != 0 || dy != 0) {
                 velocity.instant().set(new Vec3i(dx, dy, 0));
            }
        });
    }

    private Entity findPlayer() {
        var results = m_gameContext.ecs().findEntitiesWith(PlayerController.class);
        var it = results.iterator();
        if (it.hasNext()) {
            return it.next().entity();
        }
        return null;
    }
}
