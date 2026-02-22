package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.MapDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;

public class MapFileIO {
    private static final String MAPS_DIR = "core/src/main/resources/content/maps";

    private final ObjectMapper m_yamlMapper;
    private final JFileChooser m_fileChooser;
    private File m_currentFile;

    public MapFileIO() {
        var yamlFactory = YAMLFactory.builder()
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
                .build();
        m_yamlMapper = new ObjectMapper(yamlFactory);
        m_yamlMapper.findAndRegisterModules();

        m_fileChooser = new JFileChooser();
        m_fileChooser.setFileFilter(
                new FileNameExtensionFilter("YAML files", "yaml", "yml"));
        File mapsDir = findMapsDir();
        if (mapsDir != null) {
            m_fileChooser.setCurrentDirectory(mapsDir);
        }
    }

    public MapDefinition load(Component parent) {
        int result = m_fileChooser.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File file = m_fileChooser.getSelectedFile();
        return loadFile(file, parent);
    }

    public MapDefinition loadFile(File file, Component parent) {
        try {
            var def = m_yamlMapper.readValue(file, MapDefinition.class);
            m_currentFile = file;
            return def;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                    "Failed to load map: " + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public boolean save(Component parent, EditorModel model) {
        if (m_currentFile == null) {
            return saveAs(parent, model);
        }
        return writeFile(m_currentFile, model, parent);
    }

    public boolean saveAs(Component parent, EditorModel model) {
        if (m_currentFile != null) {
            m_fileChooser.setSelectedFile(m_currentFile);
        }
        int result = m_fileChooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        File file = m_fileChooser.getSelectedFile();
        if (!file.getName().endsWith(".yaml")
                && !file.getName().endsWith(".yml")) {
            file = new File(file.getAbsolutePath() + ".yaml");
        }

        return writeFile(file, model, parent);
    }

    private boolean writeFile(
            File file,
            EditorModel model,
            Component parent) {
        try {
            var def = model.toMapDefinition();
            m_yamlMapper.writeValue(file, def);
            m_currentFile = file;
            model.clearDirty();
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                    "Failed to save map: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public File getCurrentFile() {
        return m_currentFile;
    }

    public void clearCurrentFile() {
        m_currentFile = null;
    }

    private File findMapsDir() {
        File dir = new File(MAPS_DIR);
        if (dir.isDirectory()) {
            return dir;
        }
        File userDir = new File(
                java.lang.System.getProperty("user.dir"), MAPS_DIR);
        if (userDir.isDirectory()) {
            return userDir;
        }
        return null;
    }
}
