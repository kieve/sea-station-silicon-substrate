package ca.kieve.ssss.util;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Socketable;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.event.EjectEvent;

public final class SocketUtil {
    private SocketUtil() {
    }

    public static void handleSocketedDeath(GameContext context, Entity bodyEntity, Socket socket) {
        var playerEntity = socket.socketedEntity;
        if (playerEntity == null) {
            return;
        }

        var socketPlug = playerEntity.get(SocketPlug.class);
        if (socketPlug == null) {
            return;
        }

        context.log().log("Your robotic body is destroyed! You are forcibly ejected!");

        socket.destroyed = true;

        if (bodyEntity.has(Socketable.class)) {
            bodyEntity.removeType(Socketable.class);
        }

        context.events().addEvent(new EjectEvent(playerEntity, socketPlug, bodyEntity, socket));
    }
}
