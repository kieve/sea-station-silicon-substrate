package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.SystemConfigSaver;
import ca.kieve.ssss.editor.model.SystemConfigModel;
import ca.kieve.ssss.editor.ui.fx.EditorButton;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;
import ca.kieve.ssss.editor.util.DialogUtil;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.util.List;

public class SystemConfigPanel extends BorderPane {
    private static final int TOOLBAR_SPACING = 8;
    private static final int TOOLBAR_PADDING = 8;
    private static final int FORM_HGAP = 10;
    private static final int FORM_VGAP = 10;
    private static final int FORM_PADDING = 16;

    private final SystemConfigModel m_model;

    public SystemConfigPanel(SystemConfigModel model, List<String> availableMaps) {
        m_model = model;

        setTop(createToolbar());
        setCenter(createForm(availableMaps));
    }

    public SystemConfigModel getModel() {
        return m_model;
    }

    public void save() {
        if (m_model.getFile() == null) {
            return;
        }
        try {
            SystemConfigSaver.save(m_model, m_model.getFile());
        } catch (IOException ex) {
            var alert = new Alert(Alert.AlertType.ERROR);
            DialogUtil.style(alert, "Failed to Save System Config");
            alert.setContentText(ex.getMessage());
            alert.showAndWait();
        }
    }

    private HBox createToolbar() {
        var saveBtn = new EditorButton("Save");
        saveBtn.disableProperty().bind(m_model.modifiedProperty().not());
        saveBtn.setOnAction(e -> save());

        var toolbar = new HBox(TOOLBAR_SPACING, saveBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(TOOLBAR_PADDING));
        return toolbar;
    }

    private GridPane createForm(List<String> availableMaps) {
        var grid = new GridPane();
        grid.setHgap(FORM_HGAP);
        grid.setVgap(FORM_VGAP);
        grid.setPadding(new Insets(FORM_PADDING));

        var launchMapLabel = new EditorLabel("Launch Map");
        m_model.launchMapDirtyProperty().addListener(
                (obs, oldVal, newVal) ->
                        launchMapLabel.setText(newVal
                                ? "Launch Map *"
                                : "Launch Map"));

        var launchMapCombo = new ComboBox<>(
                FXCollections.observableArrayList(availableMaps));
        launchMapCombo.valueProperty()
                .bindBidirectional(m_model.launchMapProperty());

        grid.add(launchMapLabel, 0, 0);
        grid.add(launchMapCombo, 1, 0);

        return grid;
    }
}
