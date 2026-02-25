package ca.kieve.ssss.editor.ui.fx;

import javafx.scene.control.Label;

/**
 * A {@link Label} that disables mnemonic parsing so underscores
 * in text are displayed literally.
 */
public class EditorLabel extends Label {
    public EditorLabel() {
        setMnemonicParsing(false);
    }

    public EditorLabel(String text) {
        super(text);
        setMnemonicParsing(false);
    }
}
