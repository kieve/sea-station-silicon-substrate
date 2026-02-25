package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.EditorTheme;
import ca.kieve.ssss.editor.ui.fx.EditorCheckBox;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

public class ToolOptionsPanel extends VBox {
    // language=css
    private static final String CSS =
            EditorTheme.OVERLAY_CSS;

    private final CheckBox m_allLayersCheck;

    public ToolOptionsPanel() {
        getStylesheets().add(inline(CSS));
        getStyleClass().add(EditorTheme.STYLE_OVERLAY);
        setSpacing(4);
        setMaxWidth(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);

        var header = new EditorLabel("Paint Options");
        header.setStyle("-fx-font-weight: bold;");

        m_allLayersCheck = new EditorCheckBox("All Layers");
        m_allLayersCheck.setFocusTraversable(false);

        getChildren().addAll(header, m_allLayersCheck);
        setVisible(false);
        setManaged(false);
    }

    public void updateForTool(EditorToolBar.Tool tool) {
        boolean show = tool == EditorToolBar.Tool.PAINT;
        setVisible(show);
        setManaged(show);
    }

    public BooleanProperty allLayersProperty() {
        return m_allLayersCheck.selectedProperty();
    }

    public boolean isAllLayers() {
        return m_allLayersCheck.isSelected();
    }
}
