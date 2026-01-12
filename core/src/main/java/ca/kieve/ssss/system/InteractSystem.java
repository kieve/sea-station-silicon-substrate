package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Attackable;
import ca.kieve.ssss.component.Examinable;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.Socketable;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.event.AttackEvent;
import ca.kieve.ssss.event.Event;
import ca.kieve.ssss.event.ExamineEvent;
import ca.kieve.ssss.event.SocketEvent;
import ca.kieve.ssss.util.Vec3i;
import dev.dominion.ecs.api.Entity;

/**
 * System that detects when the player bumps into interactable entities
 * and creates events for other systems to consume.
 *
 * Uses marker components (Attackable, Socketable, Examinable) to dynamically
 * determine available interactions based on entity state.
 */
public class InteractSystem extends System {
    public InteractSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void tick() {
        // Find the player entity
        var playerResults = m_gameContext.ecs().findEntitiesWith(Player.class, Position.class);
        var optionalPlayer = playerResults.stream().findFirst();
        if (optionalPlayer.isEmpty()) {
            return;
        }

        var playerWith = optionalPlayer.get();
        var playerEntity = playerWith.entity();
        var socketPlug = playerEntity.get(SocketPlug.class);

        // Determine which entity to check for velocity (player or socketed body)
        var controlledEntity = (socketPlug != null && socketPlug.currentBody != null)
            ? socketPlug.currentBody
            : playerEntity;

        var velocity = controlledEntity.get(Velocity.class);
        var position = controlledEntity.get(Position.class);

        if (velocity == null || position == null) {
            return;
        }

        var instantVelocity = velocity.instant();
        if (instantVelocity.equals(Vec3i.ZERO)) {
            return;
        }

        var targetPos = position.getPosition().add(instantVelocity);
        var entitiesAtTarget = m_gameContext.pos().getAt(targetPos);

        var eventContext = m_gameContext.events();
        boolean shouldBlockMovement = false;

        for (var entity : entitiesAtTarget) {
            var event = resolveInteraction(controlledEntity, socketPlug, entity);
            if (event == null) {
                continue;
            }

            eventContext.addEvent(event);
            shouldBlockMovement = true;
        }

        // Cancel movement if blocked by any interaction
        if (shouldBlockMovement) {
            instantVelocity.set(Vec3i.ZERO);
        }
    }

    /**
     * Resolves which interaction should occur based on the entity's components and state.
     * Priority order: Attack (if alive) > Socket (if dead or no health) > Examine
     */
    private Event resolveInteraction(Entity controlledEntity, SocketPlug socketPlug, Entity target) {
        // Check attackable first - only if entity has health and is alive
        if (target.has(Attackable.class)) {
            var health = target.get(Health.class);
            if (health != null && health.hp > 0) {
                return new AttackEvent(controlledEntity, target);
            }
        }

        // Check socketable - for dead entities or entities without health
        // Only allow socketing if player is not already socketed
        if (target.has(Socketable.class) && socketPlug.currentBody == null) {
            // Cannot socket into a destroyed mech
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

        // Check examinable - lowest priority
        if (target.has(Examinable.class)) {
            return new ExamineEvent(target);
        }

        return null;
    }
}
