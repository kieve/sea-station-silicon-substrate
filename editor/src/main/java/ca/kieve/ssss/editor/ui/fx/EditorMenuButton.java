package ca.kieve.ssss.editor.ui.fx;

import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

/**
 * A {@link MenuButton} that disables mnemonic parsing so
 * underscores in text are displayed literally.
 */
public class EditorMenuButton extends MenuButton {
    public EditorMenuButton(String text, Node graphic, MenuItem... items) {
        super(text, graphic, items);
        setMnemonicParsing(false);
    }
}
