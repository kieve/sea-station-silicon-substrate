package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

public class EditorToolBar extends VBox {
    public enum Tool {
        PAINT,
        SELECT
    }

    private static final String STYLE_EDITOR_TOOL_BAR =
            "editor-tool-bar";
    private static final String STYLE_TOOL_BUTTON =
            "editor-tool-button";

    // language=css
    private static final String CSS = """
            .%1$s {
                -fx-background-color: -color-bg-subtle;
                -fx-padding: 4;
                -fx-spacing: 2;
            }
            .%2$s {
                -fx-background-color: transparent;
                -fx-background-radius: 4;
                -fx-pref-width: 28;
                -fx-pref-height: 28;
                -fx-min-width: 28;
                -fx-min-height: 28;
                -fx-padding: 0;
                -fx-font-size: 12;
                -fx-cursor: hand;
            }
            .%2$s:selected {
                -fx-background-color: -color-accent-muted;
            }
            .%2$s:hover {
                -fx-background-color: -color-neutral-muted;
            }
            """.formatted(STYLE_EDITOR_TOOL_BAR, STYLE_TOOL_BUTTON);

    private final ObjectProperty<Tool> m_activeTool =
            new SimpleObjectProperty<>(Tool.PAINT);

    public EditorToolBar() {
        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_EDITOR_TOOL_BAR);
        setPrefWidth(36);
        setMinWidth(36);
        setMaxWidth(36);

        var toggleGroup = new ToggleGroup();

        var paintBtn = new ToggleButton("P");
        paintBtn.getStyleClass().add(STYLE_TOOL_BUTTON);
        paintBtn.setToggleGroup(toggleGroup);
        paintBtn.setSelected(true);
        paintBtn.setTooltip(new Tooltip("Paint"));
        paintBtn.setFocusTraversable(false);

        paintBtn.setOnAction(e -> {
            m_activeTool.set(Tool.PAINT);
            paintBtn.setSelected(true);
        });

        var selectBtn = new ToggleButton("S");
        selectBtn.getStyleClass().add(STYLE_TOOL_BUTTON);
        selectBtn.setToggleGroup(toggleGroup);
        selectBtn.setTooltip(new Tooltip("Select"));
        selectBtn.setFocusTraversable(false);

        selectBtn.setOnAction(e -> {
            m_activeTool.set(Tool.SELECT);
            selectBtn.setSelected(true);
        });

        getChildren().addAll(paintBtn, selectBtn);
    }

    public ObjectProperty<Tool> activeToolProperty() {
        return m_activeTool;
    }

    public Tool getActiveTool() {
        return m_activeTool.get();
    }
}
