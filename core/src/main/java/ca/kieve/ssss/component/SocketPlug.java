package ca.kieve.ssss.component;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.annotations.EditorIgnore;

/**
 * Marks an entity as a "microchip" that can plug into Socket components.
 * Typically attached to the player entity.
 * Holds a reference to the current body the player is socketed into (if any).
 */
public class SocketPlug implements Component {
    /**
     * The body entity the player is currently socketed into, or null if not socketed.
     */
    @EditorIgnore
    public Entity currentBody;
}
