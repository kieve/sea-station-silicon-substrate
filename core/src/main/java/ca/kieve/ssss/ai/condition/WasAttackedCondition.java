package ca.kieve.ssss.ai.condition;

import java.util.Map;

import ca.kieve.ssss.component.LastAttacker;

/**
 * Condition that returns true if the entity was attacked (has LastAttacker component).
 */
public class WasAttackedCondition implements Condition {
    @Override
    public void initialize(Map<String, Object> properties) {
    }

    @Override
    public boolean evaluate(ConditionContext context) {
        return context.entity().has(LastAttacker.class);
    }
}
