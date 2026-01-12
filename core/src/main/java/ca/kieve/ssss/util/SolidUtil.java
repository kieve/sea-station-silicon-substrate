package ca.kieve.ssss.util;

import ca.kieve.ssss.component.Solid;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import dev.dominion.ecs.api.Entity;

/**
 * Utility methods for checking solid entities at positions.
 */
public final class SolidUtil {
    private SolidUtil() {}

    /**
     * Checks if there is any solid entity at the given position.
     */
    public static boolean hasSolid(GameContext context, Vec3i pos) {
        for (Entity entity : context.pos().getAt(pos)) {
            if (entity.has(Solid.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there is a static solid (wall/block) at the given position.
     * Static solids are entities with Solid but without Velocity.
     * Use this for wall-following logic that should ignore moving entities.
     */
    public static boolean hasWall(GameContext context, Vec3i pos) {
        for (Entity entity : context.pos().getAt(pos)) {
            if (entity.has(Solid.class) && !entity.has(Velocity.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there is a moving solid entity at the given position.
     * Moving solids are entities with both Solid and Velocity.
     */
    public static boolean hasMovingSolid(GameContext context, Vec3i pos) {
        for (Entity entity : context.pos().getAt(pos)) {
            if (entity.has(Solid.class) && entity.has(Velocity.class)) {
                return true;
            }
        }
        return false;
    }
}
