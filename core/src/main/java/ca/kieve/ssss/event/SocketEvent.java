package ca.kieve.ssss.event;

import dev.dominion.ecs.api.Entity;

/**
 * Event data for SOCKET events.
 * Created by InteractSystem when player bumps into a socketable body.
 * Consumed by SocketSystem.
 */
public record SocketEvent(Entity target) implements Event {}
