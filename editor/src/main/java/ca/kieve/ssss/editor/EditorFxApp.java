package ca.kieve.ssss.editor;

import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.editor.component.EntityDetailPanel;
import ca.kieve.ssss.editor.component.EntityListPanel;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

public class EditorFxApp extends Application {
    @Override
    public void start(Stage stage) {
        ContentRegistry registry = EditorApp.getRegistry();

        var detailPanel = new EntityDetailPanel(registry);
        var listPanel = new EntityListPanel(registry, detailPanel::showEntity);

        var splitPane = new SplitPane(listPanel, detailPanel);
        splitPane.setDividerPositions(0.3);

        var scene = new Scene(splitPane, 900, 600);
        stage.setTitle("Entity Browser");
        stage.setScene(scene);
        stage.show();
    }
}
