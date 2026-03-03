package ca.kieve.ssss.content;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class ComponentTypeSerializer extends JsonSerializer<Class<?>> {
    @Override
    public void serialize(
        Class<?> value,
        JsonGenerator gen,
        SerializerProvider serializers
    ) throws IOException {
        gen.writeString(value.getSimpleName());
    }
}
