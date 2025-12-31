package ca.kieve.ssss.ai.state;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.util.Vec3i;
import com.github.yellowstonegames.grid.Coord;
import dev.dominion.ecs.api.Entity;

/**
 * State that pathfinds toward the target using the shared PathingContext.
 * Logic ported from AiChaserSystem.
 */
public class ChaseState extends AiState {
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
        var velocity = context.entity().get(Velocity.class);
        if (entityPosComp == null || velocity == null) {
            return;
        }

        Vec3i pos = entityPosComp.getPosition();
        Vec3i targetPos = targetPosComp.getPosition();

        // Only chase on same Z level
        if (pos.z != targetPos.z) {
            return;
        }

        // Stop if adjacent to target
        int dist = Math.abs(pos.x - targetPos.x) + Math.abs(pos.y - targetPos.y);
        if (dist <= 1) {
            return;
        }

        // Use shared PathingContext for pathfinding
        Coord next = context.gameContext().pathing().findNextStep(pos, targetPos);
        if (next == null) {
            return;
        }

        int dx = next.x - pos.x;
        int dy = next.y - pos.y;

        if (dx != 0 || dy != 0) {
            velocity.instant().set(new Vec3i(dx, dy, 0));
        }
    }
}
