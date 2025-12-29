package ca.kieve.ssss.event;

import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.SocketPlug;
import dev.dominion.ecs.api.Entity;

/**
 * Event for system-to-system communication when a player ejects from a socketed body.
 * Created by EjectSystem, consumed by SocketSystem.
 */
public record EjectEvent(
    Entity playerEntity,
    SocketPlug socketPlug,
    Entity bodyEntity,
    Socket socket
) implements SystemEvent {}
