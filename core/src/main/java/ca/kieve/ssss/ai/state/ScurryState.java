package ca.kieve.ssss.ai.state;

import static ca.kieve.ssss.util.Vec3i.EAST;
import static ca.kieve.ssss.util.Vec3i.NORTH;
import static ca.kieve.ssss.util.Vec3i.SOUTH;
import static ca.kieve.ssss.util.Vec3i.WEST;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.ScurryConfig;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.SolidUtil;
import ca.kieve.ssss.util.Vec3i;

/**
 * AI state that makes an entity run in a straight line until hitting a wall,
 * then follow the wall in a configurable direction (clockwise or counter-clockwise).
 * Handles both concave and convex corners.
 *
 * Configuration is read from the ScurryConfig component on the entity.
 * If no ScurryConfig exists, defaults to clockwise=true with a random direction.
 */
public class ScurryState extends AiState {
    // Direction rotation order: NORTH -> EAST -> SOUTH -> WEST -> NORTH (clockwise)
    private static final Vec3i[] DIRECTIONS = {
        NORTH,
        EAST,
        SOUTH,
        WEST
    };

    private Vec3i m_direction;
    private boolean m_clockwise;
    private boolean m_wallFollowing;
    private boolean m_initialized;

    @Override
    public void onEnter(StateContext context) {
        if (m_initialized) {
            return;
        }
        m_initialized = true;

        // Read configuration from entity's ScurryConfig component
        ScurryConfig config = context.entity().get(ScurryConfig.class);
        if (config != null) {
            m_clockwise = config.clockwise();
            m_direction = config.initialDirection() != null
                ? config.initialDirection().copy()
                : pickRandomDirection(context.gameContext());
        } else {
            // Default configuration
            m_clockwise = pickRandomClockwise(context.gameContext());
            m_direction = pickRandomDirection(context.gameContext());
        }

        m_wallFollowing = false;
    }

    private Vec3i pickRandomDirection(GameContext gameContext) {
        int idx = gameContext.random().nextInt(DIRECTIONS.length);
        return DIRECTIONS[idx].copy();
    }

    private boolean pickRandomClockwise(GameContext gameContext) {
        return gameContext.random().nextBoolean();
    }

    @Override
    public void execute(StateContext context) {
        Entity entity = context.entity();
        Position posComp = entity.get(Position.class);
        Velocity velocity = entity.get(Velocity.class);

        if (posComp == null || velocity == null) {
            return;
        }

        Vec3i pos = posComp.getPosition();
        GameContext gameContext = context.gameContext();

        if (!m_wallFollowing) {
            executeStraightLine(gameContext, pos, velocity, entity);
        } else {
            executeWallFollowing(gameContext, pos, velocity, entity);
        }
    }

    private void executeStraightLine(
            GameContext gameContext,
            Vec3i pos,
            Velocity velocity,
            Entity entity) {
        Vec3i ahead = pos.add(m_direction);

        if (SolidUtil.hasWallFor(gameContext, ahead, entity)) {
            // Hit wall - enter wall-following mode
            m_wallFollowing = true;
            // Turn: CW turns right, CCW turns left
            if (m_clockwise) {
                m_direction = rotateClockwise(m_direction);
            } else {
                m_direction = rotateCounterClockwise(m_direction);
            }
            // Don't move this tick - let next tick handle movement
        } else if (SolidUtil.hasMovingSolidFor(gameContext, ahead, entity)) {
            // Hit moving entity - pick new random direction to find a wall
            m_direction = pickRandomDirection(gameContext);
            // Don't move this tick
        } else {
            // Path clear - move forward
            velocity.instant().set(m_direction);
        }
    }

    private void executeWallFollowing(
            GameContext gameContext,
            Vec3i pos,
            Velocity velocity,
            Entity entity) {
        // Wall should be on the opposite side of our turn direction:
        // CW (turn right) -> wall on left
        // CCW (turn left) -> wall on right
        Vec3i wallDir = m_clockwise
            ? rotateCounterClockwise(m_direction)
            : rotateClockwise(m_direction);
        Vec3i wallPos = pos.add(wallDir);
        Vec3i ahead = pos.add(m_direction);

        // Check if a moving entity blocks our path - exit wall-following mode
        if (SolidUtil.hasMovingSolidFor(gameContext, ahead, entity)) {
            m_wallFollowing = false;
            m_direction = pickRandomDirection(gameContext);
            // Don't move this tick
            return;
        }

        if (!SolidUtil.hasWallFor(gameContext, wallPos, entity)) {
            // Convex corner - wall disappeared, turn toward where wall was
            if (m_clockwise) {
                m_direction = rotateCounterClockwise(m_direction);
            } else {
                m_direction = rotateClockwise(m_direction);
            }
            velocity.instant().set(m_direction);
        } else if (SolidUtil.hasWallFor(gameContext, ahead, entity)) {
            // Concave corner - wall blocks path, turn away
            if (m_clockwise) {
                m_direction = rotateClockwise(m_direction);
            } else {
                m_direction = rotateCounterClockwise(m_direction);
            }
            // Don't move this tick
        } else {
            // Normal following - move forward
            velocity.instant().set(m_direction);
        }
    }

    private static Vec3i rotateClockwise(Vec3i dir) {
        int idx = getDirectionIndex(dir);
        return DIRECTIONS[(idx + 1) % DIRECTIONS.length].copy();
    }

    private static Vec3i rotateCounterClockwise(Vec3i dir) {
        int idx = getDirectionIndex(dir);
        return DIRECTIONS[(idx + DIRECTIONS.length - 1) % DIRECTIONS.length].copy();
    }

    private static int getDirectionIndex(Vec3i dir) {
        for (int i = 0; i < DIRECTIONS.length; i++) {
            if (dir.x == DIRECTIONS[i].x && dir.y == DIRECTIONS[i].y) {
                return i;
            }
        }
        return 0;
    }
}
