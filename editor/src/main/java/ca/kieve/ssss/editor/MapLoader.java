package ca.kieve.ssss.editor;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import ca.kieve.ssss.content.MapDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class MapLoader {
    private static final ObjectMapper MAPPER = JsonMapper.builder(new YAMLFactory())
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .findAndAddModules()
        .build();

    public static MapDefinition load(File mapFile) throws IOException {
        return MAPPER.readValue(mapFile, MapDefinition.class);
    }

    public static MapDefinition load(InputStream stream) throws IOException {
        return MAPPER.readValue(stream, MapDefinition.class);
    }
}
