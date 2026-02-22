package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.editor.EditorTheme;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Map;

public class ComponentBox extends VBox {
    public ComponentBox(ComponentDefinition comp) {
        super(4);
        getStyleClass().add(EditorTheme.STYLE_COMPONENT_BOX);

        var typeLabel = new Label(comp.type().getSimpleName());
        typeLabel.getStyleClass().add(EditorTheme.STYLE_COMPONENT_TYPE);
        getChildren().add(typeLabel);

        Map<String, Object> props = comp.properties();
        if (props.isEmpty()) {
            var marker = new Label("(marker component)");
            marker.getStyleClass().add(
                    EditorTheme.STYLE_COMPONENT_MARKER);
            getChildren().add(marker);
        } else {
            for (var entry : props.entrySet()) {
                getChildren().add(
                    new Label(entry.getKey() + ": " + entry.getValue()));
            }
        }
    }
}
