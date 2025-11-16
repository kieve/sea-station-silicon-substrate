package ca.kieve.ssss.system;

import ca.kieve.ssss.component.InteractComponent;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;

/**
 * System that detects when the player bumps into interactable entities
 * and creates events for other systems to consume.
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
            var interact = entity.get(InteractComponent.class);
            if (interact == null) {
                continue;
            }

            // Create event for this interaction
            eventContext.addEvent(interact.eventType, entity);

            // Track if any interaction blocks movement
            if (interact.blocksMovement) {
                shouldBlockMovement = true;
            }
        }

        // Cancel movement if blocked by any interaction
        if (shouldBlockMovement) {
            instantVelocity.set(Vec3i.ZERO);
        }
    }
}
