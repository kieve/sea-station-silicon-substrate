package ca.kieve.ssss.editor.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import ca.kieve.ssss.editor.ui.fx.EditorToggleButton;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

public class EditorToolBar extends VBox {
    public enum Tool {
        PAINT,
        SELECT,
        MOVE
    }

    private static final String STYLE_EDITOR_TOOL_BAR = "editor-tool-bar";
    private static final String STYLE_TOOL_BUTTON = "editor-tool-button";
    private static final String ICON_STROKE_STYLE =
        "-fx-stroke: -color-fg-default; -fx-stroke-width: 1;";
    private static final String ICON_FILL_STYLE = "-fx-fill: -color-fg-default;";

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

    private final ObjectProperty<Tool> m_activeTool = new SimpleObjectProperty<>(Tool.SELECT);
    private final BooleanProperty m_composedMode = new SimpleBooleanProperty(false);

    public EditorToolBar() {
        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_EDITOR_TOOL_BAR);
        setPrefWidth(36);
        setMinWidth(36);
        setMaxWidth(36);

        var toggleGroup = new ToggleGroup();

        var selectBtn = new EditorToggleButton();
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

        var moveBtn = new EditorToggleButton();
        moveBtn.setGraphic(createMoveIcon());
        moveBtn.getStyleClass().add(STYLE_TOOL_BUTTON);
        moveBtn.setToggleGroup(toggleGroup);
        moveBtn.setTooltip(new Tooltip("Move"));
        moveBtn.setFocusTraversable(false);

        moveBtn.setOnAction(e -> {
            m_activeTool.set(Tool.MOVE);
            moveBtn.setSelected(true);
        });

        var paintBtn = new EditorToggleButton();
        paintBtn.setGraphic(createPaintIcon());
        paintBtn.getStyleClass().add(STYLE_TOOL_BUTTON);
        paintBtn.setToggleGroup(toggleGroup);
        paintBtn.setTooltip(new Tooltip("Paint"));
        paintBtn.setFocusTraversable(false);

        paintBtn.setOnAction(e -> {
            m_activeTool.set(Tool.PAINT);
            paintBtn.setSelected(true);
        });

        var separator = new Separator();
        VBox.setMargin(separator, new Insets(4, 2, 4, 2));

        var composedBtn = new EditorToggleButton();
        composedBtn.setGraphic(createComposedIcon());
        composedBtn.getStyleClass().add(STYLE_TOOL_BUTTON);
        composedBtn.setTooltip(new Tooltip("Composed view (read-only)"));
        composedBtn.setFocusTraversable(false);
        composedBtn.selectedProperty().bindBidirectional(m_composedMode);

        getChildren().addAll(selectBtn, moveBtn, paintBtn, separator, composedBtn);
    }

    private static Node createComposedIcon() {
        // Two overlapping squares to suggest layered composition
        var back = new Rectangle(2, 2, 9, 9);
        back.setStyle("-fx-fill: transparent;" + ICON_STROKE_STYLE);
        var front = new Rectangle(6, 6, 9, 9);
        front.setStyle("-fx-fill: transparent;" + ICON_STROKE_STYLE);
        return new Group(back, front);
    }

    private static Node createSelectIcon() {
        // Arrow cursor pointing up-left
        var arrow = new Polygon(0, 0, 0, 12, 3.5, 9, 7, 14, 9, 13, 5.5, 8, 9.5, 8);
        arrow.setStyle(ICON_FILL_STYLE + ICON_STROKE_STYLE + "-fx-stroke-width: 0.5;");
        return arrow;
    }

    private static Node createPaintIcon() {
        // Paintbrush: angled handle + bristle tip
        var handle = new Line(12, 0, 4, 8);
        handle.setStyle(ICON_STROKE_STYLE + "-fx-stroke-width: 2;");

        var bristles = new Rectangle(1, 8, 6, 5);
        bristles.setStyle(ICON_FILL_STYLE);
        bristles.setArcWidth(2);
        bristles.setArcHeight(2);

        return new Group(handle, bristles);
    }

    private static Node createMoveIcon() {
        // Four-directional arrow
        double cx = 7;
        double cy = 7;
        double arm = 5;
        double head = 2.5;

        var vLine = new Line(cx, cy - arm, cx, cy + arm);
        vLine.setStyle(ICON_STROKE_STYLE);
        var hLine = new Line(cx - arm, cy, cx + arm, cy);
        hLine.setStyle(ICON_STROKE_STYLE);

        String style = ICON_FILL_STYLE + ICON_STROKE_STYLE
            + "-fx-stroke-width: 0.5;";
        var up = new Polygon(cx, cy - arm, cx - head, cy - arm + head, cx + head, cy - arm + head);
        up.setStyle(style);
        var down = new Polygon(
            cx,
            cy + arm,
            cx - head,
            cy + arm - head,
            cx + head,
            cy + arm - head
        );
        down.setStyle(style);
        var left = new Polygon(
            cx - arm,
            cy,
            cx - arm + head,
            cy - head,
            cx - arm + head,
            cy + head
        );
        left.setStyle(style);
        var right = new Polygon(
            cx + arm,
            cy,
            cx + arm - head,
            cy - head,
            cx + arm - head,
            cy + head
        );
        right.setStyle(style);

        return new Group(vLine, hLine, up, down, left, right);
    }

    public ObjectProperty<Tool> activeToolProperty() {
        return m_activeTool;
    }

    public Tool getActiveTool() {
        return m_activeTool.get();
    }

    public BooleanProperty composedModeProperty() {
        return m_composedMode;
    }

    public boolean isComposedMode() {
        return m_composedMode.get();
    }
}
