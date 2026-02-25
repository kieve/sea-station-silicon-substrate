package ca.kieve.ssss.editor.ui.fx;

import javafx.scene.control.Button;

/**
 * A {@link Button} that disables mnemonic parsing so underscores
 * in text are displayed literally.
 */
public class EditorButton extends Button {
    public EditorButton() {
        setMnemonicParsing(false);
    }

    public EditorButton(String text) {
        super(text);
        setMnemonicParsing(false);
    }
}
