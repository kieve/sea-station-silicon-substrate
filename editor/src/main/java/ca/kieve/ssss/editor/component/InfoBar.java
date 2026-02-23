package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.EditorTheme;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class InfoBar extends HBox {
    private final Label m_fileLabel;
    private final Label m_dimensionsLabel;

    public InfoBar() {
        getStyleClass().add(EditorTheme.STYLE_INFO_BAR);
        setAlignment(Pos.CENTER_LEFT);

        m_fileLabel = new Label();
        m_dimensionsLabel = new Label();

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
