package ca.kieve.ssss.component;

import ca.kieve.ssss.annotations.EditorIgnore;

import dev.dominion.ecs.api.Entity;

/**
 * Marks an entity as a robotic body that can be controlled by a SocketPlug entity.
 * Holds a reference to the player entity currently socketed in (if any).
 *
 * When a player sockets into this body, damage is dealt to socketedHp instead of
 * the body's Health component. When socketedHp reaches 0, the player is forcibly
 * ejected and the body becomes permanently destroyed (cannot be re-socketed).
 */
public class Socket implements Component {
    /**
     * The player entity currently socketed into this body, or null if empty.
     */
    @EditorIgnore
    public Entity socketedEntity;

    /**
     * Maximum HP pool available when the body is being controlled.
     */
    public long socketedMaxHp;

    /**
     * Current HP when the body is being controlled. When this reaches 0,
     * the player is ejected and the body is permanently destroyed.
     */
    public long socketedHp;

    /**
     * When true, this body has been destroyed while socketed and cannot
     * be re-entered. The Socketable component should also be removed.
     */
    public boolean destroyed = false;

    public Socket() {
        this(100, 100);
    }

    public Socket(long socketedMaxHp, long socketedHp) {
        this.socketedMaxHp = socketedMaxHp;
        this.socketedHp = socketedHp;
    }
}
