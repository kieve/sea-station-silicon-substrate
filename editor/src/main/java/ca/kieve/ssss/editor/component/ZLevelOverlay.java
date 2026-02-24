package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.EditorTheme;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

public class ZLevelOverlay extends HBox {
    private static final int SPACING = 4;
    private static final int COMBO_WIDTH = 56;
    private static final int OVERLAY_PAD_X = 8;
    private static final int OVERLAY_PAD_Y = 4;
    private static final int OVERLAY_RADIUS = 6;
    private static final String STYLE_Z_STEP_BUTTON =
            "editor-z-step-button";
    private static final String STYLE_Z_ADD_BUTTON =
            "editor-z-add-button";

    // language=css
    private static final String CSS =
            EditorTheme.OVERLAY_CSS + """
            .%1$s {
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
            .%1$s:hover {
                -fx-background-color: -color-neutral-muted;
            }
            .%2$s {
                -fx-background-color: transparent;
                -fx-background-radius: 0 %3$d %3$d 0;
                -fx-padding: 0;
                -fx-font-size: 18;
                -fx-cursor: hand;
            }
            .%2$s:hover {
                -fx-background-color: -color-neutral-muted;
            }
            """.formatted(
                    STYLE_Z_STEP_BUTTON,
                    STYLE_Z_ADD_BUTTON,
                    OVERLAY_RADIUS);

    private final ComboBox<Integer> m_zCombo;
    private List<Integer> m_zLevels = List.of();
    private Consumer<Integer> m_onZLevelRequested;
    private Runnable m_onAddLayerRequested;
    private boolean m_suppressCallback;

    public ZLevelOverlay() {
        getStylesheets().add(inline(CSS));
        getStyleClass().add(EditorTheme.STYLE_OVERLAY);
        setAlignment(Pos.CENTER);
        setSpacing(SPACING);
        setMaxWidth(USE_PREF_SIZE);
        setMaxHeight(USE_PREF_SIZE);

        var label = new Label("Z:");
        m_zCombo = new ComboBox<>();
        m_zCombo.setPrefWidth(COMBO_WIDTH);
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
            if (m_suppressCallback) {
                return;
            }
            Integer val = m_zCombo.getValue();
            if (val != null
                    && m_onZLevelRequested != null) {
                m_onZLevelRequested.accept(val);
            }
        });

        var addBtn = new Button("+");
        addBtn.setFocusTraversable(false);
        addBtn.getStyleClass().add(STYLE_Z_ADD_BUTTON);
        addBtn.setMaxHeight(Double.MAX_VALUE);
        addBtn.minWidthProperty().bind(
                addBtn.heightProperty());
        addBtn.prefWidthProperty().bind(
                addBtn.heightProperty());
        addBtn.setOnAction(e -> {
            if (m_onAddLayerRequested != null) {
                m_onAddLayerRequested.run();
            }
        });
        HBox.setMargin(addBtn, new Insets(
                -OVERLAY_PAD_Y, -OVERLAY_PAD_X,
                -OVERLAY_PAD_Y, 0));

        getChildren().addAll(
                label, m_zCombo, stepButtons,
                addBtn);
    }

    public void setOnZLevelRequested(
            Consumer<Integer> callback) {
        m_onZLevelRequested = callback;
    }

    public void setOnAddLayerRequested(
            Runnable callback) {
        m_onAddLayerRequested = callback;
    }

    public void setZLevels(
            List<Integer> zLevels, int defaultZ) {
        m_zLevels = zLevels;
        m_suppressCallback = true;
        m_zCombo.getItems().setAll(zLevels);
        m_zCombo.setValue(defaultZ);
        m_suppressCallback = false;
    }

    public void displayZLevel(int z) {
        m_suppressCallback = true;
        m_zCombo.setValue(z);
        m_suppressCallback = false;
    }

    private void step(int direction) {
        if (m_zLevels.isEmpty()) {
            return;
        }
        Integer current = m_zCombo.getValue();
        if (current == null) {
            return;
        }
        int idx = m_zLevels.indexOf(current);
        int next = (idx + direction + m_zLevels.size())
                % m_zLevels.size();
        int newZ = m_zLevels.get(next);
        if (m_onZLevelRequested != null) {
            m_onZLevelRequested.accept(newZ);
        }
    }
}
