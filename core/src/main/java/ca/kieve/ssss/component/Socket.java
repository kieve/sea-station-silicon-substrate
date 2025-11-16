package ca.kieve.ssss.component;

import dev.dominion.ecs.api.Entity;

/**
 * Marks an entity as a robotic body that can be controlled by a SocketPlug entity.
 * Holds a reference to the player entity currently socketed in (if any).
 */
public class Socket implements Component {
    /**
     * The player entity currently socketed into this body, or null if empty.
     */
    public Entity socketedEntity;
}
