package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.SystemConfig;
import ca.kieve.ssss.editor.model.SystemConfigModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;

public final class SystemConfigSaver {
    private static final ObjectMapper MAPPER = new ObjectMapper(
            YAMLFactory.builder()
                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                    .build())
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public static void save(SystemConfigModel model, File file) throws IOException {
        var config = new SystemConfig(model.getLaunchMap());
        MAPPER.writeValue(file, config);
        model.setFile(file);
        model.markSaved();
    }

    private SystemConfigSaver() {}
}
