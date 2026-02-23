package ca.kieve.ssss.editor;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.editor.component.EditorTitleBar;
import ca.kieve.ssss.editor.component.EntityDetailPanel;
import ca.kieve.ssss.editor.component.EntityListPanel;
import ca.kieve.ssss.editor.component.MapViewPanel;
import ca.kieve.ssss.editor.ui.AppIcon;
import ca.kieve.ssss.editor.ui.WindowResizeHandler;
import ca.kieve.ssss.editor.ui.WindowsAeroSnap;
import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class EditorFxApp extends Application {
    private static final KeyCodeCombination SAVE_COMBO =
            new KeyCodeCombination(
                    KeyCode.S, KeyCombination.CONTROL_DOWN);
    private static final KeyCodeCombination SAVE_AS_COMBO =
            new KeyCodeCombination(
                    KeyCode.S,
                    KeyCombination.CONTROL_DOWN,
                    KeyCombination.SHIFT_DOWN);

    private static final String STYLE_HIDDEN_TAB_HEADER =
            "editor-hidden-tab-header";

    // language=css
    private static final String CSS = """
            .%s > .tab-header-area {
                -fx-max-height: 0;
                -fx-pref-height: 0;
                -fx-min-height: 0;
                visibility: hidden;
            }
            """.formatted(STYLE_HIDDEN_TAB_HEADER);

    private ContentRegistry m_registry;
    private TabPane m_tabPane;
    private Tab m_mapTab;
    private Stage m_stage;
    private File m_lastDirectory;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(
                new PrimerDark().getUserAgentStylesheet());

        m_stage = stage;
        m_registry = EditorApp.getRegistry();

        CompletableFuture.supplyAsync(this::resolveDefaultMapsDir)
                .thenAccept(dir -> m_lastDirectory = dir);

        var detailPanel = new EntityDetailPanel(m_registry);
        var listPanel = new EntityListPanel(
                m_registry, detailPanel::showEntity);

        var splitPane = new SplitPane(listPanel, detailPanel);
        splitPane.setDividerPositions(0.3);

        var entitiesTab = new Tab("Entities", splitPane);
        entitiesTab.setClosable(false);

        m_tabPane = new TabPane(entitiesTab);
        m_tabPane.getStylesheets().add(inline(CSS));
        m_tabPane.getStyleClass().add(STYLE_HIDDEN_TAB_HEADER);

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setMinWidth(400);
        stage.setMinHeight(300);

        stage.getIcons().addAll(
                AppIcon.create(128),
                AppIcon.create(64),
                AppIcon.create(32),
                AppIcon.create(16));

        var titleBar = new EditorTitleBar(
                stage, m_tabPane,
                this::onLoadMap,
                this::onSave,
                this::onSaveAs,
                this::onClose,
                this::checkUnsavedChanges);

        var root = new BorderPane();
        root.setTop(titleBar);
        root.setCenter(m_tabPane);

        var scene = new Scene(root, 900, 600);

        // Keyboard shortcuts
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (SAVE_AS_COMBO.match(e)) {
                onSaveAs();
                e.consume();
            } else if (SAVE_COMBO.match(e)) {
                onSave();
                e.consume();
            }
        });

        // TAB to cycle tabs
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() != KeyCode.TAB) {
                return;
            }
            int count = m_tabPane.getTabs().size();
            if (count < 2) {
                return;
            }
            int cur = m_tabPane.getSelectionModel()
                    .getSelectedIndex();
            int next = e.isShiftDown()
                    ? (cur - 1 + count) % count
                    : (cur + 1) % count;
            m_tabPane.getSelectionModel().select(next);
            e.consume();
        });

        // Intercept window close for unsaved changes check
        stage.setOnCloseRequest(this::handleCloseRequest);

        WindowResizeHandler.install(scene, stage);

        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.show();
        stage.setAlwaysOnTop(false);

        WindowsAeroSnap.apply(stage);
    }

    private MapViewPanel getMapViewPanel() {
        if (m_mapTab == null) {
            return null;
        }
        if (m_mapTab.getContent() instanceof MapViewPanel mvp) {
            return mvp;
        }
        return null;
    }

    private void onSave() {
        var panel = getMapViewPanel();
        if (panel == null) {
            return;
        }
        if (panel.getModel().getFile() != null) {
            panel.save();
        } else {
            onSaveAs();
        }
    }

    private void onSaveAs() {
        var panel = getMapViewPanel();
        if (panel == null) {
            return;
        }

        var chooser = new FileChooser();
        chooser.setTitle("Save Map As");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "YAML files", "*.yaml"));

        File currentFile = panel.getModel().getFile();
        if (currentFile != null) {
            chooser.setInitialDirectory(
                    currentFile.getParentFile());
            chooser.setInitialFileName(currentFile.getName());
        } else if (m_lastDirectory != null
                && m_lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(m_lastDirectory);
        }

        File file = chooser.showSaveDialog(m_stage);
        if (file == null) {
            return;
        }

        m_lastDirectory = file.getParentFile();
        panel.saveAs(file);
        m_mapTab.setText("Map - " + file.getName());
    }

    private void onClose() {
        var closeEvent = new WindowEvent(
                m_stage, WindowEvent.WINDOW_CLOSE_REQUEST);
        m_stage.fireEvent(closeEvent);
    }

    private void handleCloseRequest(WindowEvent e) {
        if (!checkUnsavedChanges()) {
            e.consume();
        }
    }

    /**
     * Check for unsaved changes and prompt the user.
     * Returns true if it's safe to proceed (save/discard),
     * false if cancelled.
     */
    private boolean checkUnsavedChanges() {
        var panel = getMapViewPanel();
        if (panel == null || !panel.getModel().isModified()) {
            return true;
        }

        var saveBtn = new ButtonType(
                "Save", ButtonBar.ButtonData.YES);
        var dontSaveBtn = new ButtonType(
                "Don't Save", ButtonBar.ButtonData.NO);
        var cancelBtn = new ButtonType(
                "Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        var alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "You have unsaved changes. "
                        + "Do you want to save before closing?",
                saveBtn, dontSaveBtn, cancelBtn);
        alert.setHeaderText("Unsaved Changes");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancelBtn) {
            return false;
        }
        if (result.get() == saveBtn) {
            onSave();
            // If still modified after save attempt (e.g. cancelled
            // Save As dialog), don't close
            if (panel.getModel().isModified()) {
                return false;
            }
        }
        return true;
    }

    private void onLoadMap() {
        var chooser = new FileChooser();
        chooser.setTitle("Load Map");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "YAML files", "*.yaml"));

        if (m_lastDirectory != null
                && m_lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(m_lastDirectory);
        }

        File file = chooser.showOpenDialog(m_stage);
        if (file == null) {
            return;
        }

        m_lastDirectory = file.getParentFile();
        loadMapFile(file);
    }

    private void loadMapFile(File file) {
        try {
            MapDefinition mapDef = MapLoader.load(file);
            var mapViewPanel =
                    new MapViewPanel(m_registry, mapDef, file);

            if (m_mapTab == null) {
                m_mapTab = new Tab();
                m_mapTab.setOnClosed(e -> m_mapTab = null);
                m_tabPane.getTabs().add(m_mapTab);
            }

            m_mapTab.setContent(mapViewPanel);
            m_mapTab.setText("Map - " + file.getName());

            // Bind modified state to tab text
            mapViewPanel.getModel().modifiedProperty()
                    .addListener((obs, oldVal, newVal) -> {
                String name = mapViewPanel.getModel().getFile()
                        != null
                        ? mapViewPanel.getModel().getFile()
                                .getName()
                        : "untitled";
                m_mapTab.setText("Map - " + name
                        + (newVal ? " *" : ""));
            });

            m_tabPane.getSelectionModel().select(m_mapTab);
        } catch (IOException ex) {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load map");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private File resolveDefaultMapsDir() {
        String mapsRelPath =
                "core/src/main/resources/content/maps";

        Path userDir = Path.of(
                System.getProperty("user.dir"));
        Path fromUserDir = userDir.resolve(mapsRelPath);
        if (fromUserDir.toFile().isDirectory()) {
            return fromUserDir.toFile();
        }

        Path fromParent =
                userDir.getParent().resolve(mapsRelPath);
        if (fromParent.toFile().isDirectory()) {
            return fromParent.toFile();
        }

        return null;
    }
}
