package ca.kieve.ssss.content;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;
import java.util.Map;

public class ComponentDefinition {
    private final Class<?> m_type;
    private final Map<String, Object> m_properties;

    @JsonCreator
    public ComponentDefinition(
        @JsonProperty("type")
        @JsonDeserialize(using = ComponentTypeDeserializer.class)
        Class<?> type
    ) {
        m_type = type;
        m_properties = new HashMap<>();
    }

    public Class<?> type() {
        return m_type;
    }

    public Map<String, Object> properties() {
        return m_properties;
    }

    @JsonAnySetter
    public void setProperty(String key, Object value) {
        m_properties.put(key, value);
    }
}
