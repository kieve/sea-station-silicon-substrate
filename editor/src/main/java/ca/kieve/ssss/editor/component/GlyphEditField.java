package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.GlyphDefinition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.Map;

public class GlyphEditField extends HBox {
    private final Map<String, GlyphDefinition> m_glyphs;
    private final Label m_label;
    private final Button m_button;

    private Character m_selectedChar;

    public GlyphEditField(
            Map<String, GlyphDefinition> glyphs) {
        super(6);
        m_glyphs = glyphs;
        setAlignment(Pos.CENTER_LEFT);

        m_label = new Label("Not set");
        m_button = new Button("Set");
        m_button.setOnAction(e -> openSelector());

        getChildren().addAll(m_label, m_button);
    }

    public void setGlyphChar(char c) {
        m_selectedChar = c;
        updateDisplay();
    }

    public Character getLayoutChar() {
        return m_selectedChar;
    }

    private void openSelector() {
        var dialog = new GlyphSelectorDialog(m_glyphs);
        dialog.showAndWait().ifPresent(selection -> {
            m_selectedChar =
                    selection.definition().character();
            updateDisplay();
        });
    }

    private void updateDisplay() {
        if (m_selectedChar == null) {
            m_label.setText("Not set");
            m_button.setText("Set");
            return;
        }

        String glyphName = findGlyphName(m_selectedChar);
        if (glyphName != null) {
            m_label.setText("'" + m_selectedChar
                    + "' (" + glyphName + ")");
        } else {
            m_label.setText("'" + m_selectedChar + "'");
        }
        m_button.setText("Edit");
    }

    private String findGlyphName(char c) {
        for (var entry : m_glyphs.entrySet()) {
            if (entry.getValue().character() == c) {
                return entry.getKey();
            }
        }
        return null;
    }
}
