package ca.kieve.ssss.ai.state;

import ca.kieve.ssss.component.Equipment;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.event.AttackEvent;
import ca.kieve.ssss.util.Vec3i;

import dev.dominion.ecs.api.Entity;

/**
 * State that attacks the target when adjacent.
 * Logic ported from AiAttackerSystem.
 */
public class AttackState extends AiState {
    @Override
    public void execute(StateContext context) {
        Entity target = context.targetEntity();
        if (target == null) {
            return;
        }

        var targetPosComp = target.get(Position.class);
        if (targetPosComp == null) {
            return;
        }

        var entityPosComp = context.entity().get(Position.class);
        if (entityPosComp == null) {
            return;
        }

        Vec3i pos = entityPosComp.getPosition();
        Vec3i targetPos = targetPosComp.getPosition();

        // Only attack on same Z level
        if (pos.z != targetPos.z) {
            return;
        }

        // Check if adjacent (Manhattan distance == 1)
        if (pos.manhattanDistTo(targetPos) != 1) {
            return;
        }

        // Verify entity has equipment
        if (!context.entity().has(Equipment.class)) {
            return;
        }

        // Fire attack event
        context.gameContext().events().addEvent(
            new AttackEvent(context.entity(), target));
    }
}
