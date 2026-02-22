package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.MapDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public final class MapLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public static MapDefinition load(File mapFile) throws IOException {
        return MAPPER.readValue(mapFile, MapDefinition.class);
    }
}
