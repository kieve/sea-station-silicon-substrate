package ca.kieve.ssss.event;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.SocketPlug;

/**
 * Event data for EJECT events.
 * Created by EjectSystem, consumed by SocketSystem.
 */
public record EjectEvent(
    Entity playerEntity,
    SocketPlug socketPlug,
    Entity bodyEntity,
    Socket socket
) implements Event {
}
