package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.ComponentDefinition;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Map;

public class ComponentBox extends VBox {
    public ComponentBox(ComponentDefinition comp) {
        super(4);
        setPadding(new Insets(8));
        setBorder(new Border(new BorderStroke(
            Color.GRAY,
            BorderStrokeStyle.SOLID,
            new CornerRadii(4),
            BorderWidths.DEFAULT)));

        var typeLabel = new Label(comp.type().getSimpleName());
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        getChildren().add(typeLabel);

        Map<String, Object> props = comp.properties();
        if (props.isEmpty()) {
            var marker = new Label("(marker component)");
            marker.setStyle("-fx-text-fill: gray;");
            getChildren().add(marker);
        } else {
            for (var entry : props.entrySet()) {
                getChildren().add(
                    new Label(entry.getKey() + ": " + entry.getValue()));
            }
        }
    }
}
