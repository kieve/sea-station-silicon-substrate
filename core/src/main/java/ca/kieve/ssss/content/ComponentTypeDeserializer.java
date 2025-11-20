package ca.kieve.ssss.content;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class ComponentTypeDeserializer extends JsonDeserializer<Class<?>> {
    private static final String COMPONENT_PACKAGE = "ca.kieve.ssss.component.";

    @Override
    public Class<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String typeName = p.getText();
        try {
            return Class.forName(COMPONENT_PACKAGE + typeName);
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to load component class: " + typeName, e);
        }
    }
}
