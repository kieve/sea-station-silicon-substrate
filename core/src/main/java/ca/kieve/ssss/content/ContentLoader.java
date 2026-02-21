package ca.kieve.ssss.content;

import ca.kieve.ssss.ai.behavior.BehaviorDefinition;
import ca.kieve.ssss.component.Identifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContentLoader {
    private static final String CONTENT_PATH = "content/";
    private static final String FONTS_FILE = "fonts.yaml";
    private static final String GLYPHS_FILE = "glyphs.yaml";
    private static final String BEHAVIORS_FILE = "behaviors.yaml";
    private static final String ENTITIES_PATH = "entities/";
    private static final String FILE_LIST = "file_list.txt";
    private static final String DIR_LIST = "dir_list.txt";

    private final ObjectMapper m_yamlMapper;
    private final ContentRegistry m_registry;

    public ContentLoader() {
        m_yamlMapper = new ObjectMapper(new YAMLFactory());
        m_yamlMapper.findAndRegisterModules();
        m_registry = new ContentRegistry();
    }

    public ContentRegistry loadAll() {
        loadFonts();
        loadGlyphs();
        loadBehaviors();
        loadAllEntities();
        return m_registry;
    }

    public ContentRegistry getRegistry() {
        return m_registry;
    }

    private void loadFonts() {
        FileHandle file = Gdx.files.internal(CONTENT_PATH + FONTS_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            FontsFile data = m_yamlMapper.readValue(file.readString(), FontsFile.class);
            if (data.fonts != null) {
                for (Map.Entry<String, FontDefinition> entry : data.fonts.entrySet()) {
                    m_registry.registerFont(entry.getKey(), entry.getValue());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + FONTS_FILE, e);
        }
    }

    private void loadGlyphs() {
        FileHandle file = Gdx.files.internal(CONTENT_PATH + GLYPHS_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            GlyphsFile data = m_yamlMapper.readValue(file.readString(), GlyphsFile.class);
            if (data.glyphs != null) {
                for (Map.Entry<String, GlyphDefinition> entry : data.glyphs.entrySet()) {
                    m_registry.registerGlyph(entry.getKey(), entry.getValue());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + GLYPHS_FILE, e);
        }
    }

    private void loadBehaviors() {
        FileHandle file = Gdx.files.internal(CONTENT_PATH + BEHAVIORS_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            var iterator = m_yamlMapper.readValues(
                m_yamlMapper.getFactory().createParser(file.readString()),
                BehaviorDefinition.class
            );

            while (iterator.hasNext()) {
                BehaviorDefinition def = iterator.next();
                m_registry.registerBehavior(def.id(), def);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + BEHAVIORS_FILE, e);
        }
    }

    private void loadAllEntities() {
        var entityFiles = new ArrayList<String>();
        for (String dir : readIndex(ENTITIES_PATH + DIR_LIST)) {
            entityFiles.addAll(
                readFileIndex(ENTITIES_PATH + dir + "/"));
        }
        loadEntityFiles(entityFiles);
    }

    private List<String> readIndex(String indexPath) {
        FileHandle indexFile = Gdx.files.internal(CONTENT_PATH + indexPath);
        if (!indexFile.exists()) {
            return List.of();
        }

        List<String> entries = new ArrayList<>();
        for (String line : indexFile.readString().split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        return entries;
    }

    private List<String> readFileIndex(String directory) {
        List<String> files = new ArrayList<>();
        for (String filename : readIndex(directory + FILE_LIST)) {
            files.add(directory + filename);
        }
        return files;
    }

    private void loadEntityFiles(List<String> filenames) {
        for (String filename : filenames) {
            FileHandle file = Gdx.files.internal(CONTENT_PATH + filename);
            if (!file.exists()) {
                continue;
            }

            try {
                var iterator = m_yamlMapper.readValues(
                    m_yamlMapper.getFactory().createParser(file.readString()),
                    EntityDefinition.class
                );

                while (iterator.hasNext()) {
                    EntityDefinition def = iterator.next();
                    String entityId = extractEntityId(def);
                    m_registry.registerEntity(entityId, def);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load " + filename, e);
            }
        }
    }

    private String extractEntityId(EntityDefinition def) {
        for (ComponentDefinition comp : def.components()) {
            if (comp.type() == Identifier.class) {
                Object key = comp.properties().get("key");
                if (key != null) {
                    return key.toString();
                }
            }
        }
        throw new RuntimeException(
            "Entity definition missing Identifier component with 'key' property");
    }

    public static class GlyphsFile {
        public Map<String, GlyphDefinition> glyphs;
    }

    public static class FontsFile {
        public Map<String, FontDefinition> fonts;
    }
}
