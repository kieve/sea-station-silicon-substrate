package ca.kieve.ssss.ai.state;

import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.SolidUtil;
import ca.kieve.ssss.util.Vec3i;
import dev.dominion.ecs.api.Entity;

import static ca.kieve.ssss.util.Vec3i.EAST;
import static ca.kieve.ssss.util.Vec3i.NORTH;
import static ca.kieve.ssss.util.Vec3i.SOUTH;
import static ca.kieve.ssss.util.Vec3i.WEST;

/**
 * State that moves away from the target entity.
 * Evaluates all cardinal directions and picks the one that maximizes distance.
 */
public class FleeState extends AiState {
    private static final Vec3i[] DIRECTIONS = { NORTH, EAST, SOUTH, WEST };

    @Override
    public void execute(StateContext context) {
        Entity target = context.targetEntity();
        if (target == null) {
            return;
        }

        var targetPosComp = target.get(Position.class);
        var entityPosComp = context.entity().get(Position.class);
        var velocity = context.entity().get(Velocity.class);
        if (targetPosComp == null || entityPosComp == null || velocity == null) {
            return;
        }

        Vec3i pos = entityPosComp.getPosition();
        Vec3i targetPos = targetPosComp.getPosition();
        GameContext gameContext = context.gameContext();

        // Only flee on same Z level
        if (pos.z != targetPos.z) {
            return;
        }

        // Find best direction that increases distance from target
        Vec3i bestDir = null;
        int bestDistance = pos.manhattanDistTo(targetPos);

        for (Vec3i dir : DIRECTIONS) {
            Vec3i newPos = pos.add(dir);

            // Skip if blocked (entity-aware for size restrictions)
            if (SolidUtil.isBlockedFor(gameContext, newPos, context.entity())) {
                continue;
            }

            int newDistance = newPos.manhattanDistTo(targetPos);
            if (newDistance > bestDistance) {
                bestDistance = newDistance;
                bestDir = dir;
            }
        }

        // If found a good direction, move there
        if (bestDir != null) {
            velocity.instant().set(bestDir);
        }
    }
}
