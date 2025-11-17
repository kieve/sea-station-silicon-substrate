package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Attackable;
import ca.kieve.ssss.component.Examinable;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Socketable;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.event.EventType;
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
        var playerResults = m_gameContext.ecs().findEntitiesWith(
            SocketPlug.class,
            Position.class
        );

        var optionalPlayer = playerResults.stream().findFirst();
        if (optionalPlayer.isEmpty()) {
            return;
        }

        var playerWith = optionalPlayer.get();
        var socketPlug = playerWith.comp1();

        // Determine which entity to check for velocity (player or socketed body)
        var controlledEntity = socketPlug.currentBody != null
            ? socketPlug.currentBody
            : playerWith.entity();

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
            var interaction = resolveInteraction(socketPlug, entity);
            if (interaction == null) {
                continue;
            }

            // Create event for this interaction
            eventContext.addEvent(interaction, entity);

            // All resolved interactions block movement
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
    private EventType resolveInteraction(SocketPlug socketPlug, Entity entity) {
        // Check attackable first - only if entity has health and is alive
        if (entity.has(Attackable.class)) {
            var health = entity.get(Health.class);
            if (health != null && health.hp > 0) {
                return EventType.ATTACK;
            }
        }

        // Check socketable - for dead entities or entities without health
        // Only allow socketing if player is not already socketed
        if (entity.has(Socketable.class) && socketPlug.currentBody == null) {
            var health = entity.get(Health.class);
            if (health == null || health.hp <= 0) {
                return EventType.SOCKET;
            }
        }

        // Check examinable - lowest priority
        if (entity.has(Examinable.class)) {
            return EventType.EXAMINE;
        }

        return null;
    }
}
