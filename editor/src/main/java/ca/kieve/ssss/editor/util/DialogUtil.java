package ca.kieve.ssss.editor.util;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.editor.ui.AppIcon;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;

public final class DialogUtil {
    // AtlantaFX sets :no-header > .content padding to
    // "1em 1em 0 0", expecting the graphic-container to
    // provide left spacing. Since we remove the graphic,
    // restore symmetric left padding.
    // language=css
    private static final String CSS = """
            .dialog-pane:no-header > .content {
                -fx-padding: 1em 1em 0 1em;
            }
            """;

    public static void style(Alert alert, String title) {
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.getDialogPane().getStylesheets()
                .add(inline(CSS));

        String ch = switch (alert.getAlertType()) {
            case CONFIRMATION -> "?";
            case WARNING -> "!";
            case ERROR -> "X";
            case INFORMATION -> "i";
            default -> "@";
        };

        alert.setOnShown(e -> {
            var stage = (Stage) alert.getDialogPane()
                    .getScene().getWindow();
            stage.getIcons().setAll(
                    AppIcon.create(ch, 32),
                    AppIcon.create(ch, 16));
        });
    }

    public static void style(
            Dialog<?> dialog, String title) {
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.getDialogPane().getStylesheets()
                .add(inline(CSS));

        dialog.setOnShown(e -> {
            var stage = (Stage) dialog.getDialogPane()
                    .getScene().getWindow();
            stage.getIcons().setAll(
                    AppIcon.create(32),
                    AppIcon.create(16));
        });
    }

    private DialogUtil() {}
}
