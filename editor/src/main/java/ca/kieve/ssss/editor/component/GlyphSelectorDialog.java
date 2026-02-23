package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.content.GlyphDefinition;
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

    private static final String STYLE_GLYPH_SELECTOR_BUTTON =
            "glyph-selector-button";
    private static final String STYLE_GLYPH_SELECTOR_CHAR =
            "glyph-selector-char";
    private static final String STYLE_GLYPH_SELECTOR_NAME =
            "glyph-selector-name";

    // language=css
    private static final String CSS = """
            .%1$s {
                -fx-background-color: -color-bg-subtle;
                -fx-background-radius: 4;
                -fx-border-color: -color-border-default;
                -fx-border-radius: 4;
                -fx-border-width: 1;
                -fx-pref-width: 64;
                -fx-pref-height: 64;
                -fx-min-width: 64;
                -fx-min-height: 64;
                -fx-padding: 4;
                -fx-cursor: hand;
            }
            .%1$s:hover {
                -fx-background-color: -color-accent-muted;
                -fx-border-color: -color-accent-fg;
            }
            .%2$s {
                -fx-font-size: 22;
                -fx-font-weight: bold;
            }
            .%3$s {
                -fx-font-size: 9;
                -fx-text-fill: -color-fg-muted;
            }
            """.formatted(
            STYLE_GLYPH_SELECTOR_BUTTON,
            STYLE_GLYPH_SELECTOR_CHAR,
            STYLE_GLYPH_SELECTOR_NAME);

    public GlyphSelectorDialog(
            Map<String, GlyphDefinition> glyphs) {
        setTitle("Select Glyph");
        setResizable(true);

        getDialogPane().getStylesheets().add(inline(CSS));

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
                    STYLE_GLYPH_SELECTOR_CHAR);

            var nameLabel = new Label(id);
            nameLabel.getStyleClass().add(
                    STYLE_GLYPH_SELECTOR_NAME);

            var cell = new VBox(2, charLabel, nameLabel);
            cell.setAlignment(Pos.CENTER);

            var btn = new Button();
            btn.setGraphic(cell);
            btn.getStyleClass().add(
                    STYLE_GLYPH_SELECTOR_BUTTON);
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
