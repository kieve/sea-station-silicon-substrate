package ca.kieve.ssss.ai.behavior;

import ca.kieve.ssss.ai.state.AiState;
import ca.kieve.ssss.content.ReflectionFactory;

/**
 * Factory for creating behavior-related objects from definitions.
 */
public class BehaviorFactory {
    public AiState createState(StateDefinition definition) {
        // Include priority in properties so states can access it if needed
        definition.properties().put("_priority", definition.priority());
        return ReflectionFactory.createState(definition.state(), definition.properties());
    }
}
