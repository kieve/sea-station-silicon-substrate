package ca.kieve.ssss.util;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Opaque;
import ca.kieve.ssss.component.Openable;

/**
 * Utility methods for checking vision-blocking entities.
 */
public final class VisionUtil {
    private VisionUtil() {
    }

    /**
     * Checks if an entity blocks vision.
     * An entity with Opaque that is also Openable and currently open does not block vision.
     */
    public static boolean blocksVision(Entity entity) {
        if (!entity.has(Opaque.class)) {
            return false;
        }
        var openable = entity.get(Openable.class);
        return openable == null || !openable.isOpen;
    }
}
