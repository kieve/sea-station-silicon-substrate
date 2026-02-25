package ca.kieve.ssss.editor.ui.fx;

import javafx.scene.control.CheckBox;

/**
 * A {@link CheckBox} that disables mnemonic parsing so
 * underscores in text are displayed literally.
 */
public class EditorCheckBox extends CheckBox {
    public EditorCheckBox(String text) {
        super(text);
        setMnemonicParsing(false);
    }
}
