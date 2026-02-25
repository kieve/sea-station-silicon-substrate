package ca.kieve.ssss.ai.condition;

import ca.kieve.ssss.component.Health;

import java.util.Map;

/**
 * Condition that returns true if the entity is dead (HP <= 0).
 * Returns false if entity has no Health component.
 */
public class IsDeadCondition implements Condition {
    @Override
    public void initialize(Map<String, Object> properties) {
    }

    @Override
    public boolean evaluate(ConditionContext context) {
        Health health = context.entity().get(Health.class);
        if (health == null) {
            return false;
        }
        return health.hp <= 0;
    }
}
