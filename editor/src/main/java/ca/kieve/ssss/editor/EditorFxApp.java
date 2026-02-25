package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.editor.component.EditorTitleBar;
import ca.kieve.ssss.editor.component.MapViewPanel;
import ca.kieve.ssss.editor.ui.AppIcon;
import ca.kieve.ssss.editor.ui.WindowResizeHandler;
import ca.kieve.ssss.editor.ui.WindowsAeroSnap;
import ca.kieve.ssss.editor.util.DialogUtil;
import ca.kieve.ssss.editor.util.GameLauncher;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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

import static ca.kieve.ssss.editor.util.CssUtil.inline;

public class EditorFxApp extends Application {
    private static final KeyCodeCombination SAVE_COMBO =
            new KeyCodeCombination(
                    KeyCode.S,
                    KeyCombination.CONTROL_DOWN);
    private static final KeyCodeCombination SAVE_AS_COMBO =
            new KeyCodeCombination(
                    KeyCode.S,
                    KeyCombination.CONTROL_DOWN,
                    KeyCombination.SHIFT_DOWN);

    private static final String STYLE_HIDDEN_TAB_HEADER =
            "editor-hidden-tab-header";

    private static final int MIN_STAGE_WIDTH = 400;
    private static final int MIN_STAGE_HEIGHT = 300;
    private static final int ICON_128 = 128;
    private static final int ICON_64 = 64;
    private static final int ICON_32 = 32;
    private static final int ICON_16 = 16;
    private static final int SCENE_WIDTH = 1920;
    private static final int SCENE_HEIGHT = 1080;

    // language=css
    private static final String CSS = """
            .%s > .tab-header-area {
                -fx-max-height: 0;
                -fx-pref-height: 0;
                -fx-min-height: 0;
                visibility: hidden;
            }
            """.formatted(STYLE_HIDDEN_TAB_HEADER);

    private TabPane m_tabPane;
    private Tab m_mapTab;
    private Stage m_stage;
    private File m_lastDirectory;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(
                new PrimerDark()
                        .getUserAgentStylesheet());

        m_stage = stage;
        DialogUtil.setOwner(stage);

        m_lastDirectory = resolveDefaultMapsDir();

        m_tabPane = new TabPane();
        m_tabPane.getStylesheets().add(inline(CSS));
        m_tabPane.getStyleClass()
                .add(STYLE_HIDDEN_TAB_HEADER);

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setMinWidth(MIN_STAGE_WIDTH);
        stage.setMinHeight(MIN_STAGE_HEIGHT);

        stage.getIcons().addAll(
                AppIcon.create(ICON_128),
                AppIcon.create(ICON_64),
                AppIcon.create(ICON_32),
                AppIcon.create(ICON_16));

        var actions = new EditorTitleBar.Actions(
                this::onLoadMap,
                this::onSave,
                this::onSaveAs,
                this::onLaunchGame,
                this::onLaunchGameGradle,
                this::onClose);
        var titleBar = new EditorTitleBar(
                stage, m_tabPane,
                actions,
                this::checkUnsavedChanges);

        var root = new BorderPane();
        root.setTop(titleBar);
        root.setCenter(m_tabPane);

        var scene = new Scene(
                root, SCENE_WIDTH, SCENE_HEIGHT);

        // Keyboard shortcuts
        scene.addEventFilter(
                KeyEvent.KEY_PRESSED, e -> {
                    if (SAVE_AS_COMBO.match(e)) {
                        onSaveAs();
                        e.consume();
                    } else if (SAVE_COMBO.match(e)) {
                        onSave();
                        e.consume();
                    }
                });

        // TAB to cycle tabs
        scene.addEventFilter(
                KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode() != KeyCode.TAB) {
                        return;
                    }
                    int count =
                            m_tabPane.getTabs().size();
                    if (count < 2) {
                        return;
                    }
                    int cur = m_tabPane
                            .getSelectionModel()
                            .getSelectedIndex();
                    int next = e.isShiftDown()
                            ? (cur - 1 + count) % count
                            : (cur + 1) % count;
                    m_tabPane.getSelectionModel()
                            .select(next);
                    e.consume();
                });

        // Intercept window close for unsaved changes
        stage.setOnCloseRequest(
                this::handleCloseRequest);

        WindowResizeHandler.install(
                scene, stage,
                titleBar.maximizedProperty());

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
        if (m_mapTab.getContent()
                instanceof MapViewPanel mvp) {
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
            chooser.setInitialFileName(
                    currentFile.getName());
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

    private void onLaunchGame() {
        try {
            GameLauncher.launchDirect(
                    m_stage.getX(), m_stage.getY(),
                    m_stage.getWidth(),
                    m_stage.getHeight());
        } catch (IOException ex) {
            var alert =
                    new Alert(Alert.AlertType.ERROR);
            DialogUtil.style(
                    alert, "Failed to Launch Game");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private void onLaunchGameGradle() {
        try {
            GameLauncher.launchGradle(
                    m_stage.getX(), m_stage.getY(),
                    m_stage.getWidth(),
                    m_stage.getHeight());
        } catch (IOException ex) {
            var alert =
                    new Alert(Alert.AlertType.ERROR);
            DialogUtil.style(
                    alert, "Failed to Launch Game");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private void onClose() {
        var closeEvent = new WindowEvent(
                m_stage,
                WindowEvent.WINDOW_CLOSE_REQUEST);
        m_stage.fireEvent(closeEvent);
    }

    private void handleCloseRequest(WindowEvent e) {
        if (!checkUnsavedChanges()) {
            e.consume();
        }
    }

    /**
     * Check for unsaved changes and prompt the user.
     * Returns true if it's safe to proceed
     * (save/discard), false if cancelled.
     */
    private boolean checkUnsavedChanges() {
        var panel = getMapViewPanel();
        if (panel == null
                || !panel.getModel().isModified()) {
            return true;
        }

        var saveBtn = new ButtonType(
                "Save", ButtonBar.ButtonData.YES);
        var dontSaveBtn = new ButtonType(
                "Don't Save",
                ButtonBar.ButtonData.NO);
        var cancelBtn = new ButtonType(
                "Cancel",
                ButtonBar.ButtonData.CANCEL_CLOSE);

        var alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "You have unsaved changes. "
                        + "\nDo you want to save "
                        + "before closing?",
                saveBtn, dontSaveBtn, cancelBtn);
        DialogUtil.style(alert, "Unsaved Changes");

        Optional<ButtonType> result =
                alert.showAndWait();
        if (result.isEmpty()
                || result.get() == cancelBtn) {
            return false;
        }
        if (result.get() == saveBtn) {
            onSave();
            // If still modified after save attempt
            // (e.g. cancelled Save As dialog)
            return !panel.getModel().isModified();
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
                    new MapViewPanel(mapDef, file);

            if (m_mapTab == null) {
                m_mapTab = new Tab();
                m_mapTab.setOnClosed(
                        e -> m_mapTab = null);
                m_tabPane.getTabs().add(m_mapTab);
            }

            m_mapTab.setContent(mapViewPanel);
            m_mapTab.setText(
                    "Map - " + file.getName());

            // Bind modified state to tab text
            mapViewPanel.getModel().modifiedProperty()
                    .addListener(
                            (obs, oldVal, newVal) -> {
                                File f = mapViewPanel
                                        .getModel()
                                        .getFile();
                                String name = f != null
                                        ? f.getName()
                                        : "untitled";
                                m_mapTab.setText(
                                        "Map - " + name
                                        + (newVal
                                                ? " *"
                                                : ""));
                            });

            m_tabPane.getSelectionModel()
                    .select(m_mapTab);
        } catch (IOException ex) {
            var alert =
                    new Alert(Alert.AlertType.ERROR);
            DialogUtil.style(
                    alert, "Failed to Load Map");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private File resolveDefaultMapsDir() {
        String mapsRelPath =
                "core/src/main/resources/content/maps";

        Path userDir = Path.of(
                System.getProperty("user.dir"));
        Path fromUserDir =
                userDir.resolve(mapsRelPath);
        if (fromUserDir.toFile().isDirectory()) {
            return fromUserDir.toFile();
        }

        Path fromParent = userDir.getParent()
                .resolve(mapsRelPath);
        if (fromParent.toFile().isDirectory()) {
            return fromParent.toFile();
        }

        return null;
    }
}
