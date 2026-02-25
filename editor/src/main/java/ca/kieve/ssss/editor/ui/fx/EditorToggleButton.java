package ca.kieve.ssss.editor.ui.fx;

import javafx.scene.control.ToggleButton;

/**
 * A {@link ToggleButton} that disables mnemonic parsing so
 * underscores in text are displayed literally.
 */
public class EditorToggleButton extends ToggleButton {
    public EditorToggleButton() {
        setMnemonicParsing(false);
    }

    public EditorToggleButton(String text) {
        super(text);
        setMnemonicParsing(false);
    }
}
