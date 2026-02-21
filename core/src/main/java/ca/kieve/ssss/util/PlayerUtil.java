package ca.kieve.ssss.util;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With1;

import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.SocketPlug;

public final class PlayerUtil {
    private PlayerUtil() {}

    public static Entity getPlayerEntity(Dominion ecs) {
        var results = ecs.findEntitiesWith(Player.class);
        return results.stream().findFirst().map(With1::entity).orElse(null);
    }

    public static SocketPlug getSocketPlug(Dominion ecs) {
        var playerEntity = getPlayerEntity(ecs);
        if (playerEntity == null) {
            return null;
        }
        return playerEntity.get(SocketPlug.class);
    }

    public static Entity getControlledEntity(Dominion ecs) {
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

    public static Vec3i getControlledPosition(Dominion ecs) {
        var controlledEntity = getControlledEntity(ecs);
        if (controlledEntity == null) {
            return null;
        }
        var position = controlledEntity.get(Position.class);
        if (position == null) {
            return null;
        }
        return position.getPosition().copy();
    }

    public static boolean isSocketed(Dominion ecs) {
        var playerEntity = getPlayerEntity(ecs);
        if (playerEntity == null) {
            return false;
        }

        var socketPlug = playerEntity.get(SocketPlug.class);
        return socketPlug != null && socketPlug.currentBody != null;
    }
}
