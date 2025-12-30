package ca.kieve.ssss.ai.behavior;

import ca.kieve.ssss.ai.condition.Condition;
import ca.kieve.ssss.ai.state.AiState;

/**
 * Factory for creating behavior-related objects from definitions.
 */
public class BehaviorFactory {
    private static final String STATE_PACKAGE = "ca.kieve.ssss.ai.state.";
    private static final String CONDITION_PACKAGE = "ca.kieve.ssss.ai.condition.";

    public AiState createState(StateDefinition definition) {
        try {
            Class<?> stateClass = Class.forName(STATE_PACKAGE + definition.state());
            AiState state = (AiState) stateClass.getDeclaredConstructor().newInstance();
            state.initialize(definition.properties());
            return state;
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create state: " + definition.state(), e);
        }
    }

    public Condition createCondition(ConditionDefinition definition) {
        try {
            String className = definition.type();
            if (!className.endsWith("Condition")) {
                className += "Condition";
            }
            Class<?> conditionClass = Class.forName(CONDITION_PACKAGE + className);
            Condition condition = (Condition) conditionClass
                .getDeclaredConstructor().newInstance();
            condition.initialize(definition.properties());
            return condition;
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create condition: " + definition.type(), e);
        }
    }
}
