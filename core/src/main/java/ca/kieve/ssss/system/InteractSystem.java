package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.InteractionResolver;
import ca.kieve.ssss.util.PlayerUtil;
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
        var ecs = m_gameContext.ecs();
        var controlledEntity = PlayerUtil.getControlledEntity(ecs);
        if (controlledEntity == null) {
            return;
        }

        var velocity = controlledEntity.get(Velocity.class);
        var position = controlledEntity.get(Position.class);
        if (velocity == null || position == null) {
            return;
        }

        var instantVelocity = velocity.instant();
        if (instantVelocity.equals(Vec3i.ZERO)) {
            return;
        }

        var socketPlug = PlayerUtil.getSocketPlug(ecs);
        var targetPos = position.getPosition().add(instantVelocity);
        var entitiesAtTarget = m_gameContext.pos().getAt(targetPos);

        var eventContext = m_gameContext.events();
        boolean shouldBlockMovement = false;

        for (var entity : entitiesAtTarget) {
            var event = InteractionResolver.resolveBumpInteraction(
                controlledEntity, socketPlug, entity);
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
}
