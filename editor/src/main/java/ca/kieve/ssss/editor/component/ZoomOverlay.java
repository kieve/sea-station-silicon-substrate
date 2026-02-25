package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.editor.EditorTheme;
import ca.kieve.ssss.editor.ui.fx.EditorButton;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ZoomOverlay extends HBox {
    private static final String STYLE_ZOOM_RESET_BUTTON =
            "editor-zoom-reset-button";

    // language=css
    private static final String CSS =
            EditorTheme.OVERLAY_CSS + """
            .%1$s {
                -fx-background-color: transparent;
                -fx-background-radius: 3;
                -fx-padding: 0 4;
                -fx-min-width: 22;
                -fx-min-height: 20;
                -fx-pref-width: 22;
                -fx-pref-height: 20;
                -fx-font-size: 12;
                -fx-cursor: hand;
            }
            .%1$s:hover {
                -fx-background-color: -color-neutral-muted;
            }
            """.formatted(STYLE_ZOOM_RESET_BUTTON);

    private final Label m_zoomLabel;
    private Runnable m_onReset;

    public ZoomOverlay() {
        getStylesheets().add(inline(CSS));
        getStyleClass().add(EditorTheme.STYLE_OVERLAY);
        setAlignment(Pos.CENTER);
        setSpacing(4);
        setMaxWidth(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);

        m_zoomLabel = new EditorLabel("100%");
        m_zoomLabel.setMinWidth(40);
        m_zoomLabel.setAlignment(Pos.CENTER_RIGHT);

        var resetBtn = new EditorButton("\u21BA");
        resetBtn.setFocusTraversable(false);
        resetBtn.getStyleClass().add(STYLE_ZOOM_RESET_BUTTON);
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
