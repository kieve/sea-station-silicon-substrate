package ca.kieve.ssss.context;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With1;

import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.SocketPlug;

/**
 * Provides helper methods for finding and resolving player-related entities.
 * The player is the "microchip" entity with Player component.
 * When socketed, the controlled entity is the body; otherwise, it's the player.
 */
public class PlayerContext {
    /**
     * Gets the player entity (the microchip itself).
     * Returns null if no player entity exists.
     */
    public Entity getPlayerEntity(Dominion ecs) {
        var results = ecs.findEntitiesWith(Player.class);
        return results.stream().findFirst().map(With1::entity).orElse(null);
    }

    /**
     * Gets the SocketPlug component from the player entity.
     * Returns null if no player entity exists.
     */
    public SocketPlug getSocketPlug(Dominion ecs) {
        var playerEntity = getPlayerEntity(ecs);
        if (playerEntity == null) {
            return null;
        }
        return playerEntity.get(SocketPlug.class);
    }

    /**
     * Gets the entity currently being controlled by the player.
     * If socketed, returns the body; otherwise returns the player entity.
     */
    public Entity getControlledEntity(Dominion ecs) {
        var playerEntity = getPlayerEntity(ecs);
        if (playerEntity == null) {
            return null;
        }

        var socketPlug = playerEntity.get(SocketPlug.class);
        if (socketPlug == null || socketPlug.currentBody == null) {
            return playerEntity;
        }
        return socketPlug.currentBody;
    }

    /**
     * Returns true if the player is currently socketed into a body.
     */
    public boolean isSocketed(Dominion ecs) {
        var playerEntity = getPlayerEntity(ecs);
        if (playerEntity == null) {
            return false;
        }

        var socketPlug = playerEntity.get(SocketPlug.class);
        return socketPlug != null && socketPlug.currentBody != null;
    }
}
