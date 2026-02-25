package ca.kieve.ssss.editor.ui.fx;

import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

/**
 * A {@link SplitMenuButton} that disables mnemonic parsing so
 * underscores in text are displayed literally.
 */
public class EditorSplitMenuButton extends SplitMenuButton {
    public EditorSplitMenuButton(MenuItem... items) {
        super(items);
        setMnemonicParsing(false);
    }
}
