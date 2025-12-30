package ca.kieve.ssss.ai.condition;

import java.util.Map;

/**
 * Interface for AI behavior conditions.
 */
public interface Condition {
    void initialize(Map<String, Object> properties);
    boolean evaluate(ConditionContext context);
}
