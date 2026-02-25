package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.ui.fx.EditorLabel;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

public class InfoBar extends HBox {
    private static final String STYLE_INFO_BAR =
            "editor-info-bar";

    // language=css
    private static final String CSS = """
            .%s {
                -fx-background-color: -color-bg-subtle;
                -fx-padding: 2 8;
                -fx-font-size: 11;
                -fx-spacing: 8;
            }
            """.formatted(STYLE_INFO_BAR);

    private final Label m_fileLabel;
    private final Label m_dimensionsLabel;

    public InfoBar() {
        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_INFO_BAR);
        setAlignment(Pos.CENTER_LEFT);

        m_fileLabel = new EditorLabel();
        m_dimensionsLabel = new EditorLabel();

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(m_fileLabel, spacer, m_dimensionsLabel);
    }

    public void setFileName(String name) {
        m_fileLabel.setText(name);
    }

    public void setDimensions(int cols, int rows) {
        m_dimensionsLabel.setText(cols + " x " + rows);
    }
}
