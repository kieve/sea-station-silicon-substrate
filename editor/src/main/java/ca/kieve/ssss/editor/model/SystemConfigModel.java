package ca.kieve.ssss.editor.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.util.Objects;

public class SystemConfigModel {
    private final StringProperty m_launchMap = new SimpleStringProperty();
    private final BooleanProperty m_launchMapDirty = new SimpleBooleanProperty(false);
    private final BooleanProperty m_modified = new SimpleBooleanProperty(false);

    private File m_file;
    private String m_savedLaunchMap;

    public SystemConfigModel(String launchMap, File file) {
        m_file = file;
        m_savedLaunchMap = launchMap;
        m_launchMap.set(launchMap);

        m_launchMap.addListener((obs, oldVal, newVal) -> updateDirty());
    }

    public String getLaunchMap() {
        return m_launchMap.get();
    }

    public void setLaunchMap(String launchMap) {
        m_launchMap.set(launchMap);
    }

    public StringProperty launchMapProperty() {
        return m_launchMap;
    }

    public BooleanProperty launchMapDirtyProperty() {
        return m_launchMapDirty;
    }

    public boolean isModified() {
        return m_modified.get();
    }

    public BooleanProperty modifiedProperty() {
        return m_modified;
    }

    public File getFile() {
        return m_file;
    }

    public void setFile(File file) {
        m_file = file;
    }

    public void markSaved() {
        m_savedLaunchMap = m_launchMap.get();
        updateDirty();
    }

    private void updateDirty() {
        boolean launchMapDirty =
                !Objects.equals(m_launchMap.get(), m_savedLaunchMap);
        m_launchMapDirty.set(launchMapDirty);
        m_modified.set(launchMapDirty);
    }
}
