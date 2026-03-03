package ca.kieve.ssss.ai.behavior;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML-loaded state definition within a behavior.
 */
public record StateDefinition(
    String state,
    int priority,
    List<ConditionDefinition> conditions,
    Map<String, Object> properties
) {
    private static final String IS_DEAD_CONDITION = "IsDead";

    @JsonCreator
    public StateDefinition(
        @JsonProperty("state") String state,
        @JsonProperty("priority") int priority,
        @JsonProperty("conditions") List<ConditionDefinition> conditions
    ) {
        this(state, priority, conditions, new HashMap<>());
    }

    @JsonAnySetter
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public boolean hasIsDeadCondition() {
        if (conditions == null) {
            return false;
        }
        return conditions.stream()
            .anyMatch(c -> IS_DEAD_CONDITION.equals(c.type()));
    }

    /**
     * Gets the target type for this state.
     * Defaults to PLAYER if not specified.
     */
    public TargetType target() {
        String targetStr = (String) properties.get("target");
        return targetStr != null ? TargetType.valueOf(targetStr) : TargetType.PLAYER;
    }
}
