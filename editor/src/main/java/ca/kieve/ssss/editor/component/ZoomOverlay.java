package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.EditorTheme;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ZoomOverlay extends HBox {
    private final Label m_zoomLabel;
    private Runnable m_onReset;

    public ZoomOverlay() {
        getStyleClass().add(EditorTheme.STYLE_ZOOM_OVERLAY);
        setAlignment(Pos.CENTER);
        setSpacing(4);
        setMaxWidth(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);

        m_zoomLabel = new Label("100%");
        m_zoomLabel.setMinWidth(40);
        m_zoomLabel.setAlignment(Pos.CENTER_RIGHT);

        var resetBtn = new Button("\u21BA");
        resetBtn.setFocusTraversable(false);
        resetBtn.getStyleClass().add(
                EditorTheme.STYLE_ZOOM_RESET_BUTTON);
        resetBtn.setOnAction(e -> {
            if (m_onReset != null) {
                m_onReset.run();
            }
        });

        getChildren().addAll(m_zoomLabel, resetBtn);
    }

    public void bindZoom(DoubleProperty zoomProperty) {
        zoomProperty.addListener((obs, oldVal, newVal) ->
                updateLabel(newVal.doubleValue()));
        updateLabel(zoomProperty.get());
    }

    public void setOnReset(Runnable onReset) {
        m_onReset = onReset;
    }

    private void updateLabel(double zoom) {
        m_zoomLabel.setText(
                Math.round(zoom * 100) + "%");
    }
}
