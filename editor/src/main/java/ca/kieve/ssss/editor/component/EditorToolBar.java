package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.EditorTheme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

public class EditorToolBar extends VBox {
    public enum Tool {
        PAINT
    }

    private final ObjectProperty<Tool> m_activeTool =
            new SimpleObjectProperty<>(Tool.PAINT);

    public EditorToolBar() {
        getStyleClass().add(EditorTheme.STYLE_EDITOR_TOOL_BAR);
        setPrefWidth(36);
        setMinWidth(36);
        setMaxWidth(36);

        var toggleGroup = new ToggleGroup();

        var paintBtn = new ToggleButton("P");
        paintBtn.getStyleClass().add(EditorTheme.STYLE_TOOL_BUTTON);
        paintBtn.setToggleGroup(toggleGroup);
        paintBtn.setSelected(true);
        paintBtn.setTooltip(new Tooltip("Paint"));
        paintBtn.setFocusTraversable(false);

        paintBtn.setOnAction(e -> {
            m_activeTool.set(Tool.PAINT);
            paintBtn.setSelected(true);
        });

        getChildren().add(paintBtn);
    }

    public ObjectProperty<Tool> activeToolProperty() {
        return m_activeTool;
    }

    public Tool getActiveTool() {
        return m_activeTool.get();
    }
}
