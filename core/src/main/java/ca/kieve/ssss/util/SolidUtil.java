package ca.kieve.ssss.util;

import ca.kieve.ssss.component.MaxPassableSize;
import ca.kieve.ssss.component.Openable;
import ca.kieve.ssss.component.Size;
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
     * Checks if an entity is effectively solid.
     * An entity with Solid that is also Openable and currently open is not solid.
     */
    public static boolean isSolid(Entity entity) {
        if (!entity.has(Solid.class)) {
            return false;
        }
        var openable = entity.get(Openable.class);
        return openable == null || !openable.isOpen;
    }

    /**
     * Checks if there is any solid entity at the given position.
     */
    public static boolean hasSolid(GameContext context, Vec3i pos) {
        for (Entity entity : context.pos().getAt(pos)) {
            if (isSolid(entity)) {
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
            if (isSolid(entity) && !entity.has(Velocity.class)) {
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
            if (isSolid(entity) && entity.has(Velocity.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given position blocks the specified entity.
     * A position blocks an entity if it contains:
     * - A Solid entity, OR
     * - A MaxPassableSize entity where the mover's size exceeds maxSize
     */
    public static boolean isBlockedFor(GameContext context, Vec3i pos, Entity mover) {
        for (Entity entity : context.pos().getAt(pos)) {
            if (isSolid(entity)) {
                return true;
            }
            if (!canPassThrough(mover, entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there is a static wall that blocks the given entity.
     * Static walls are entities with Solid (but not Velocity), OR
     * MaxPassableSize restrictions that block this entity's size.
     */
    public static boolean hasWallFor(GameContext context, Vec3i pos, Entity mover) {
        for (Entity entity : context.pos().getAt(pos)) {
            if (entity.has(Velocity.class)) {
                continue;
            }
            if (isSolid(entity)) {
                return true;
            }
            if (!canPassThrough(mover, entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there is a moving solid or size-restricted entity at the position
     * that would block the given mover.
     */
    public static boolean hasMovingSolidFor(GameContext context, Vec3i pos, Entity mover) {
        for (Entity entity : context.pos().getAt(pos)) {
            if (!entity.has(Velocity.class)) {
                continue;
            }
            if (isSolid(entity)) {
                return true;
            }
            if (!canPassThrough(mover, entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the Size of an entity, defaulting to MEDIUM if no Size component.
     */
    public static Size getSize(Entity entity) {
        Size size = entity.get(Size.class);
        return size != null ? size : Size.MEDIUM;
    }

    /**
     * Checks if a given size can pass through a size restriction.
     * Returns true if the mover's size is at or below the maxSize.
     */
    public static boolean canSizePassThrough(Size moverSize, Size maxSize) {
        return moverSize.ordinal() <= maxSize.ordinal();
    }

    /**
     * Checks if the mover can pass through the target entity based on size restrictions.
     * Returns true if:
     * - The target has no MaxPassableSize restriction, OR
     * - The mover's size is at or below the target's maxSize
     */
    public static boolean canPassThrough(Entity mover, Entity target) {
        MaxPassableSize restriction = target.get(MaxPassableSize.class);
        if (restriction == null) {
            return true;
        }
        return canSizePassThrough(getSize(mover), restriction.maxSize());
    }
}
