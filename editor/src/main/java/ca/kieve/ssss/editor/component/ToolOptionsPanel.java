package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class ToolOptionsPanel extends VBox {
    private static final String STYLE_OVERLAY =
            "editor-tool-options-overlay";

    // language=css
    private static final String CSS = """
            .%1$s {
                -fx-background-color: rgba(30, 30, 30, 0.85);
                -fx-background-radius: 6;
                -fx-padding: 4 8;
                -fx-font-size: 11;
            }
            """.formatted(STYLE_OVERLAY);

    private final CheckBox m_allLayersCheck;

    public ToolOptionsPanel() {
        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_OVERLAY);
        setSpacing(4);
        setMaxWidth(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);

        var header = new Label("Paint Options");
        header.setStyle("-fx-font-weight: bold;");

        m_allLayersCheck = new CheckBox("All Layers");
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
