package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.editor.model.EditorMapModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;

public final class MapSaver {
    private static final ObjectMapper MAPPER = new ObjectMapper(
            YAMLFactory.builder()
                    .enable(YAMLGenerator.Feature
                            .LITERAL_BLOCK_STYLE)
                    .build())
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public static void save(EditorMapModel model, File file)
            throws IOException {
        MapDefinition def = model.toDefinition();
        MAPPER.writeValue(file, def);
        model.setFile(file);
        model.setModified(false);
    }
}
