package ca.kieve.ssss.ai.condition;

import ca.kieve.ssss.ai.behavior.TargetType;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.util.Vec3i;

import dev.dominion.ecs.api.Entity;

import java.util.Map;

/**
 * Condition that checks Manhattan distance to a target entity.
 */
public class DistanceToEntityCondition implements Condition {
    private TargetType m_targetType;
    private int m_maxDistance;

    @Override
    public void initialize(Map<String, Object> properties) {
        String target = (String) properties.get("target");
        m_targetType = TargetType.valueOf(target);
        m_maxDistance = ((Number) properties.get("distance")).intValue();
    }

    @Override
    public boolean evaluate(ConditionContext context) {
        Entity target = resolveTarget(context);
        if (target == null) {
            return false;
        }

        var targetPosComp = target.get(Position.class);
        var entityPosComp = context.entity().get(Position.class);
        if (targetPosComp == null || entityPosComp == null) {
            return false;
        }

        Vec3i targetPos = targetPosComp.getPosition();
        Vec3i entityPos = entityPosComp.getPosition();

        // Must be on same Z level
        if (targetPos.z != entityPos.z) {
            return false;
        }

        int dist = Math.abs(targetPos.x - entityPos.x)
                 + Math.abs(targetPos.y - entityPos.y);

        return dist <= m_maxDistance;
    }

    private Entity resolveTarget(ConditionContext context) {
        return context.gameContext().aiController().resolveTarget(
            context.entity(),
            m_targetType,
            context.gameContext().ecs()
        );
    }
}
