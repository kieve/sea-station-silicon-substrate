package ca.kieve.ssss.editor;

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
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class EditorFxApp extends Application {
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

        // Resolve the default maps directory in the background so it's
        // ready by the time the user opens the file chooser.
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
        m_tabPane.getStyleClass().add(
                EditorTheme.STYLE_HIDDEN_TAB_HEADER);

        // Undecorated stage — we provide our own title bar
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setMinWidth(400);
        stage.setMinHeight(300);

        // App icon: white @ on transparent background
        stage.getIcons().addAll(
                AppIcon.create(128),
                AppIcon.create(64),
                AppIcon.create(32),
                AppIcon.create(16));

        var titleBar = new EditorTitleBar(
                stage, m_tabPane, this::onLoadMap);

        var root = new BorderPane();
        root.setTop(titleBar);
        root.setCenter(m_tabPane);

        var scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(EditorTheme.STYLESHEET);

        // Use TAB to cycle tabs instead of arrow keys
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

        WindowResizeHandler.install(scene, stage);

        stage.setScene(scene);
        // Windows blocks focus-stealing, so briefly set always-on-top
        // to force the window to the front on launch.
        stage.setAlwaysOnTop(true);
        stage.show();
        stage.setAlwaysOnTop(false);

        WindowsAeroSnap.apply(stage);
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
                m_mapTab = new Tab("Map", mapViewPanel);
                m_mapTab.setOnClosed(e -> m_mapTab = null);
                m_tabPane.getTabs().add(m_mapTab);
            } else {
                m_mapTab.setContent(mapViewPanel);
            }

            m_mapTab.setText("Map - " + file.getName());
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

        // Try from user.dir (Gradle sets workingDir = assets/)
        Path userDir = Path.of(
                System.getProperty("user.dir"));
        Path fromUserDir = userDir.resolve(mapsRelPath);
        if (fromUserDir.toFile().isDirectory()) {
            return fromUserDir.toFile();
        }

        // Try from parent (if user.dir is assets/)
        Path fromParent =
                userDir.getParent().resolve(mapsRelPath);
        if (fromParent.toFile().isDirectory()) {
            return fromParent.toFile();
        }

        return null;
    }
}
