package ca.kieve.ssss.ai.behavior;

import ca.kieve.ssss.ai.state.AiState;

/**
 * Factory for creating behavior-related objects from definitions.
 */
public class BehaviorFactory {
    private static final String STATE_PACKAGE = "ca.kieve.ssss.ai.state.";

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
}
