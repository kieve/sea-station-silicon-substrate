package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.SystemConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public final class SystemConfigLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public static SystemConfig load(File file) throws IOException {
        return MAPPER.readValue(file, SystemConfig.class);
    }

    private SystemConfigLoader() {}
}
