package ca.kieve.ssss.ai.reset;

import java.util.Map;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.LastAttacker;

/**
 * Reset condition that triggers when the LastAttacker component changes.
 * This causes re-randomization when the entity is attacked by a different attacker.
 */
public class AttackerChangeReset implements ResetCondition {
    private Entity m_recordedAttacker;

    @Override
    public void initialize(Map<String, Object> properties) {
        // No properties needed for this implementation
    }

    @Override
    public boolean shouldReset(ResetContext context) {
        Entity currentAttacker = getCurrentAttacker(context.entity());
        return currentAttacker != m_recordedAttacker;
    }

    @Override
    public void recordState(ResetContext context) {
        m_recordedAttacker = getCurrentAttacker(context.entity());
    }

    private Entity getCurrentAttacker(Entity entity) {
        LastAttacker lastAttacker = entity.get(LastAttacker.class);
        return lastAttacker != null ? lastAttacker.attacker : null;
    }
}
