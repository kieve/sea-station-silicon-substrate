package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public class EditorToolBar extends VBox {
    public enum Tool {
        PAINT,
        SELECT
    }

    private static final String STYLE_EDITOR_TOOL_BAR =
            "editor-tool-bar";
    private static final String STYLE_TOOL_BUTTON =
            "editor-tool-button";
    private static final String ICON_STROKE_STYLE =
            "-fx-stroke: -color-fg-default; -fx-stroke-width: 1;";
    private static final String ICON_FILL_STYLE =
            "-fx-fill: -color-fg-default;";

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
            new SimpleObjectProperty<>(Tool.SELECT);

    public EditorToolBar() {
        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_EDITOR_TOOL_BAR);
        setPrefWidth(36);
        setMinWidth(36);
        setMaxWidth(36);

        var toggleGroup = new ToggleGroup();

        var selectBtn = new ToggleButton();
        selectBtn.setGraphic(createSelectIcon());
        selectBtn.getStyleClass().add(STYLE_TOOL_BUTTON);
        selectBtn.setToggleGroup(toggleGroup);
        selectBtn.setSelected(true);
        selectBtn.setTooltip(new Tooltip("Select"));
        selectBtn.setFocusTraversable(false);

        selectBtn.setOnAction(e -> {
            m_activeTool.set(Tool.SELECT);
            selectBtn.setSelected(true);
        });

        var paintBtn = new ToggleButton();
        paintBtn.setGraphic(createPaintIcon());
        paintBtn.getStyleClass().add(STYLE_TOOL_BUTTON);
        paintBtn.setToggleGroup(toggleGroup);
        paintBtn.setTooltip(new Tooltip("Paint"));
        paintBtn.setFocusTraversable(false);

        paintBtn.setOnAction(e -> {
            m_activeTool.set(Tool.PAINT);
            paintBtn.setSelected(true);
        });

        getChildren().addAll(selectBtn, paintBtn);
    }

    private static Node createSelectIcon() {
        // Arrow cursor pointing up-left
        var arrow = new Polygon(
                0, 0,
                0, 12,
                3.5, 9,
                7, 14,
                9, 13,
                5.5, 8,
                9.5, 8);
        arrow.setStyle(ICON_FILL_STYLE + ICON_STROKE_STYLE
                + "-fx-stroke-width: 0.5;");
        return arrow;
    }

    private static Node createPaintIcon() {
        // Paintbrush: angled handle + bristle tip
        var handle = new Line(12, 0, 4, 8);
        handle.setStyle(ICON_STROKE_STYLE
                + "-fx-stroke-width: 2;");

        var bristles = new Rectangle(1, 8, 6, 5);
        bristles.setStyle(ICON_FILL_STYLE);
        bristles.setArcWidth(2);
        bristles.setArcHeight(2);

        return new Group(handle, bristles);
    }

    public ObjectProperty<Tool> activeToolProperty() {
        return m_activeTool;
    }

    public Tool getActiveTool() {
        return m_activeTool.get();
    }
}
