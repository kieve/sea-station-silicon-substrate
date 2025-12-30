package ca.kieve.ssss.content;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.List;

public class ComponentTypeDeserializer extends JsonDeserializer<Class<?>> {
    private static final List<String> PACKAGES = List.of(
            "ca.kieve.ssss.component.",
            "ca.kieve.ssss.ai.behavior."
    );

    private static Class<?> tryLoadClass(String packagePrefix, String typeName) {
        try {
            return Class.forName(packagePrefix + typeName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public Class<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String typeName = p.getText();
        for (String pkg : PACKAGES) {
            Class<?> clazz = tryLoadClass(pkg, typeName);
            if (clazz != null) {
                return clazz;
            }
        }
        throw new IOException("Failed to load component class: " + typeName);
    }
}
