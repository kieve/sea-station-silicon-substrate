package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class ZLevelOverlay extends HBox {
    private static final String STYLE_Z_OVERLAY =
            "editor-z-overlay";
    private static final String STYLE_Z_STEP_BUTTON =
            "editor-z-step-button";

    // language=css
    private static final String CSS = """
            .%1$s {
                -fx-background-color: rgba(30, 30, 30, 0.85);
                -fx-background-radius: 6;
                -fx-padding: 4 8;
                -fx-font-size: 11;
            }
            .%2$s {
                -fx-background-color: transparent;
                -fx-background-radius: 3;
                -fx-padding: 0 3;
                -fx-min-width: 18;
                -fx-min-height: 12;
                -fx-pref-width: 18;
                -fx-pref-height: 12;
                -fx-font-size: 8;
                -fx-cursor: hand;
            }
            .%2$s:hover {
                -fx-background-color: -color-neutral-muted;
            }
            """.formatted(STYLE_Z_OVERLAY, STYLE_Z_STEP_BUTTON);

    private final ComboBox<Integer> m_zCombo;
    private final IntegerProperty m_zLevel =
            new SimpleIntegerProperty();
    private List<Integer> m_zLevels = List.of();

    public ZLevelOverlay() {
        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_Z_OVERLAY);
        setAlignment(Pos.CENTER);
        setSpacing(4);
        setMaxWidth(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);

        var label = new Label("Z:");
        m_zCombo = new ComboBox<>();
        m_zCombo.setPrefWidth(56);
        m_zCombo.setFocusTraversable(false);

        var upBtn = new Button("\u25B2");
        upBtn.setFocusTraversable(false);
        upBtn.getStyleClass().add(STYLE_Z_STEP_BUTTON);
        upBtn.setOnAction(e -> step(1));

        var downBtn = new Button("\u25BC");
        downBtn.setFocusTraversable(false);
        downBtn.getStyleClass().add(STYLE_Z_STEP_BUTTON);
        downBtn.setOnAction(e -> step(-1));

        var stepButtons = new VBox(upBtn, downBtn);
        stepButtons.setAlignment(Pos.CENTER);
        stepButtons.setSpacing(0);
        stepButtons.setPadding(Insets.EMPTY);

        m_zCombo.setOnAction(e -> {
            Integer val = m_zCombo.getValue();
            if (val != null) {
                m_zLevel.set(val);
            }
        });

        getChildren().addAll(label, m_zCombo, stepButtons);
    }

    public void setZLevels(List<Integer> zLevels, int defaultZ) {
        m_zLevels = zLevels;
        m_zCombo.getItems().setAll(zLevels);
        m_zCombo.setValue(defaultZ);
        m_zLevel.set(defaultZ);
    }

    public IntegerProperty zLevelProperty() {
        return m_zLevel;
    }

    public int getZLevel() {
        return m_zLevel.get();
    }

    public void step(int direction) {
        if (m_zLevels.isEmpty()) {
            return;
        }
        int idx = m_zLevels.indexOf(m_zLevel.get());
        int next = (idx + direction + m_zLevels.size())
                % m_zLevels.size();
        int newZ = m_zLevels.get(next);
        m_zCombo.setValue(newZ);
        m_zLevel.set(newZ);
    }
}
