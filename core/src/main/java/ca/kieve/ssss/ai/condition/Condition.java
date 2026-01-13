package ca.kieve.ssss.ai.condition;

import ca.kieve.ssss.content.YamlInitializable;

/**
 * Interface for AI behavior conditions.
 */
public interface Condition extends YamlInitializable {
    boolean evaluate(ConditionContext context);

    /**
     * Called when the state containing this condition is selected.
     * Use this to update stateful data (like MaxTimes counter).
     */
    default void onStateSelected(ConditionContext context) {}
}
