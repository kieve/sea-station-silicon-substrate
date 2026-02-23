package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.GlyphDefinition;
import ca.kieve.ssss.editor.EditorTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.Map;

public class GlyphSelectorDialog
        extends Dialog<GlyphSelectorDialog.GlyphSelection> {

    public record GlyphSelection(
            String id, GlyphDefinition definition) {}

    public GlyphSelectorDialog(
            Map<String, GlyphDefinition> glyphs) {
        setTitle("Select Glyph");
        setResizable(true);

        var flow = new FlowPane();
        flow.setHgap(6);
        flow.setVgap(6);
        flow.setPadding(new Insets(8));
        flow.setPrefWidth(400);

        glyphs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
            String id = entry.getKey();
            GlyphDefinition def = entry.getValue();

            var charLabel = new Label(
                    String.valueOf(def.character()));
            charLabel.getStyleClass().add(
                    EditorTheme.STYLE_GLYPH_SELECTOR_CHAR);

            var nameLabel = new Label(id);
            nameLabel.getStyleClass().add(
                    EditorTheme.STYLE_GLYPH_SELECTOR_NAME);

            var cell = new VBox(2, charLabel, nameLabel);
            cell.setAlignment(Pos.CENTER);

            var btn = new Button();
            btn.setGraphic(cell);
            btn.getStyleClass().add(
                    EditorTheme.STYLE_GLYPH_SELECTOR_BUTTON);
            btn.setOnAction(e -> {
                setResult(new GlyphSelection(id, def));
                close();
            });

            flow.getChildren().add(btn);
        });

        getDialogPane().setContent(flow);
        getDialogPane().getButtonTypes().add(
                ButtonType.CANCEL);

        setResultConverter(btn -> null);
    }
}
