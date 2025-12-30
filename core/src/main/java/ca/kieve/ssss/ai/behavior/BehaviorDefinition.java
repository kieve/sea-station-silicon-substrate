package ca.kieve.ssss.ai.behavior;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * YAML-loaded behavior definition.
 */
public record BehaviorDefinition(
    @JsonProperty("id") String id,
    @JsonProperty("states") List<StateDefinition> states
) {}
