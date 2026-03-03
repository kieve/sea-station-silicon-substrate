package ca.kieve.ssss.ai.behavior;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * YAML-loaded branch definition within a RandomBranchState.
 * Each branch has a name and contains nested state definitions.
 */
public record BranchDefinition(String name, List<StateDefinition> states) {
    @JsonCreator
    public BranchDefinition(
        @JsonProperty("name") String name,
        @JsonProperty("states") List<StateDefinition> states
    ) {
        this.name = name;
        this.states = states != null ? states : List.of();
    }
}
