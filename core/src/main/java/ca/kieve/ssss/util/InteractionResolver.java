package ca.kieve.ssss.util;

import ca.kieve.ssss.component.Attackable;
import ca.kieve.ssss.component.Examinable;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Lockable;
import ca.kieve.ssss.component.Openable;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Socketable;
import ca.kieve.ssss.event.AttackEvent;
import ca.kieve.ssss.event.Event;
import ca.kieve.ssss.event.ExamineEvent;
import ca.kieve.ssss.event.OpenEvent;
import ca.kieve.ssss.event.SocketEvent;

import dev.dominion.ecs.api.Entity;

/**
 * Resolves which interaction should occur when bumping into an entity.
 * Priority order: Attack (if alive) > Socket (if dead) > Open > Examine
 */
public final class InteractionResolver {
    private InteractionResolver() {}

    public static Event resolveBumpInteraction(
            Entity controlled, SocketPlug socketPlug, Entity target) {
        // Attack: only if entity has health and is alive
        if (target.has(Attackable.class)) {
            var health = target.get(Health.class);
            if (health != null && health.hp > 0) {
                return new AttackEvent(controlled, target);
            }
        }

        // Socket: for dead entities or entities without health
        // Only allow socketing if player is not already socketed
        if (target.has(Socketable.class)
                && socketPlug.currentBody == null) {
            var socket = target.get(Socket.class);
            if (socket != null && socket.destroyed) {
                // Fall through to examine instead
            } else {
                var health = target.get(Health.class);
                if (health == null || health.hp <= 0) {
                    return new SocketEvent(target);
                }
            }
        }

        // Locked entities block bump interaction
        if (target.has(Lockable.class)
                && target.get(Lockable.class).isLocked) {
            // Fall through to Examinable
        }
        // Openable: bump to open unlocked doors
        else if (target.has(Openable.class)) {
            var openable = target.get(Openable.class);
            if (!openable.isOpen) {
                return new OpenEvent(controlled, target);
            }
            // Open: player walks through (no interaction needed)
        }

        // Examine: lowest priority
        // Skip if the mover can pass through
        if (target.has(Examinable.class)) {
            if (!SolidUtil.isSolid(target)
                    && SolidUtil.canPassThrough(controlled, target)) {
                return null;
            }
            return new ExamineEvent(target);
        }

        return null;
    }
}
