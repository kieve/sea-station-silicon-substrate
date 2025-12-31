package ca.kieve.ssss.system;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.PathingContext;

/**
 * System that maintains pathfinding data.
 * Runs before AiControllerSystem to ensure pathing grids are fresh.
 *
 * Marks all pathing grids as dirty in preTick() so they will be rebuilt
 * when AI entities request paths. The actual rebuild happens lazily
 * in PathingContext.findNextStep() for efficiency.
 */
public class PathingSystem extends System {
    private final PathingContext m_pathing;

    public PathingSystem(GameContext gameContext) {
        super(gameContext);
        m_pathing = gameContext.pathing();
    }

    @Override
    public void preTick() {
        // Mark all pathing grids as dirty since entity positions may have changed.
        // Grids will be rebuilt lazily when AI entities request paths.
        m_pathing.markAllDirty();
    }
}
