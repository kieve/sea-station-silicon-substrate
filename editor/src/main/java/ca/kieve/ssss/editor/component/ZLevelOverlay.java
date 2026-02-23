package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.EditorTheme;
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
    private final ComboBox<Integer> m_zCombo;
    private final IntegerProperty m_zLevel =
            new SimpleIntegerProperty();
    private List<Integer> m_zLevels = List.of();

    public ZLevelOverlay() {
        getStyleClass().add(EditorTheme.STYLE_Z_OVERLAY);
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
        upBtn.getStyleClass().add(
                EditorTheme.STYLE_Z_STEP_BUTTON);
        upBtn.setOnAction(e -> step(1));

        var downBtn = new Button("\u25BC");
        downBtn.setFocusTraversable(false);
        downBtn.getStyleClass().add(
                EditorTheme.STYLE_Z_STEP_BUTTON);
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

    private void step(int direction) {
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
