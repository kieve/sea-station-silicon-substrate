package ca.kieve.ssss.editor.util;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import ca.kieve.ssss.editor.ui.AppIcon;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public final class DialogUtil {
    private static final String FONT_FAMILY = "Segoe UI";
    private static final int PADDING = 2;

    // AtlantaFX sets :no-header > .content padding to "1em 1em 0 0",
    // expecting the graphic-container to provide left spacing. Since
    // we remove the graphic, restore symmetric left padding.
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
        alert.getDialogPane().getStylesheets().add(inline(CSS));

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
                    renderChar(ch, 32),
                    renderChar(ch, 16));
        });
    }

    public static void style(Dialog<?> dialog, String title) {
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setGraphic(null);
        dialog.getDialogPane().getStylesheets().add(inline(CSS));

        dialog.setOnShown(e -> {
            var stage = (Stage) dialog.getDialogPane()
                    .getScene().getWindow();
            stage.getIcons().setAll(
                    AppIcon.create(32),
                    AppIcon.create(16));
        });
    }

    private static Image renderChar(String ch, int size) {
        var buf = new BufferedImage(
                size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buf.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);
        g.setComposite(AlphaComposite.SrcOver);

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);

        var refFont = new Font(FONT_FAMILY, Font.BOLD, 200);
        GlyphVector gv = refFont.createGlyphVector(
                g.getFontRenderContext(), ch);
        Rectangle2D bounds = gv.getVisualBounds();

        double available = size - PADDING * 2;
        double scale = Math.min(
                available / bounds.getWidth(),
                available / bounds.getHeight());

        double tx = (size - bounds.getWidth() * scale) / 2
                - bounds.getX() * scale;
        double ty = (size - bounds.getHeight() * scale) / 2
                - bounds.getY() * scale;

        var xform = new AffineTransform();
        xform.translate(tx, ty);
        xform.scale(scale, scale);

        g.fill(xform.createTransformedShape(gv.getOutline()));
        g.dispose();

        var img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                pw.setArgb(px, py, buf.getRGB(px, py));
            }
        }
        return img;
    }

    private DialogUtil() {}
}
