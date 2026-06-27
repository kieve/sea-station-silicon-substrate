package ca.kieve.ssss.util;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.SocketPlug;

public final class NameUtil {
    private NameUtil() {
    }

    public static String nameOf(Dominion ecs, Entity entity) {
        if (entity.has(PlayerController.class) || entity.has(Player.class)) {
            return "You";
        }

        var playerEntity = PlayerUtil.getPlayerEntity(ecs);
        if (playerEntity != null) {
            var socketPlug = playerEntity.get(SocketPlug.class);
            if (socketPlug != null && socketPlug.currentBody == entity) {
                return "You";
            }
        }

        var descriptor = entity.get(Descriptor.class);
        if (descriptor != null) {
            return descriptor.name();
        }

        return "something";
    }
}
