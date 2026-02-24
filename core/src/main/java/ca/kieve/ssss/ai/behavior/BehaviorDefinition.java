package ca.kieve.ssss.ai.behavior;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * YAML-loaded behavior definition.
 */
public record BehaviorDefinition(
    @JsonProperty("id") String id,
    @JsonProperty("states") List<StateDefinition> states
) {
    public BehaviorDefinition {
        Set<Integer> priorities = new HashSet<>();
        for (StateDefinition state : states) {
            if (!priorities.add(state.priority())) {
                throw new IllegalArgumentException(
                    "Duplicate priority " + state.priority() + " in behavior " + id);
            }
        }
    }
}
