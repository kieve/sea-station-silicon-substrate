package ca.kieve.ssss.ai.behavior;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * YAML-loaded condition definition.
 */
public record ConditionDefinition(
        String type,
        Map<String, Object> properties
) {
    @JsonCreator
    public ConditionDefinition(@JsonProperty("type") String type) {
        this(type, new HashMap<>());
    }

    @JsonAnySetter
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
}
