package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.editor.ui.fx.EditorLabel;

import java.util.Map;
import javafx.scene.layout.VBox;

public class ComponentBox extends VBox {
    private static final String STYLE_COMPONENT_BOX =
            "editor-component-box";
    private static final String STYLE_COMPONENT_TYPE =
            "editor-component-type";
    private static final String STYLE_COMPONENT_MARKER =
            "editor-component-marker";

    // language=css
    private static final String CSS = """
            .%1$s {
                -fx-padding: 8;
                -fx-border-color: gray;
                -fx-border-style: solid;
                -fx-border-radius: 4;
                -fx-border-width: 1;
            }
            .%2$s {
                -fx-font-weight: bold;
                -fx-font-size: 13;
            }
            .%3$s {
                -fx-text-fill: gray;
            }
            """.formatted(
            STYLE_COMPONENT_BOX,
            STYLE_COMPONENT_TYPE,
            STYLE_COMPONENT_MARKER);

    public ComponentBox(ComponentDefinition comp) {
        super(4);
        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_COMPONENT_BOX);

        var typeLabel = new EditorLabel(comp.type().getSimpleName());
        typeLabel.getStyleClass().add(STYLE_COMPONENT_TYPE);
        getChildren().add(typeLabel);

        Map<String, Object> props = comp.properties();
        if (props.isEmpty()) {
            var marker = new EditorLabel("(marker component)");
            marker.getStyleClass().add(STYLE_COMPONENT_MARKER);
            getChildren().add(marker);
        } else {
            for (var entry : props.entrySet()) {
                getChildren().add(
                    new EditorLabel(entry.getKey() + ": " + entry.getValue()));
            }
        }
    }
}
