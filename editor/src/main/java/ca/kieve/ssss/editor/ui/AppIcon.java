package ca.kieve.ssss.editor.ui;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Generates icons: white text on a transparent background.
 * Uses AWT rendering so it works before the JavaFX scene
 * is shown.
 */
public final class AppIcon {
    private static final String FONT_FAMILY = "Segoe UI";
    private static final int PADDING = 2;

    /**
     * Create the default app icon (@) at the given size.
     *
     * @param size pixel width and height of the icon
     */
    public static Image create(int size) {
        return create("@", size);
    }

    /**
     * Render a string as a white-on-transparent icon.
     *
     * @param text the text to render
     * @param size pixel width and height of the icon
     */
    public static Image create(String text, int size) {
        var buf = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buf.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);
        g.setComposite(AlphaComposite.SrcOver);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);

        // Render at a large reference size, then measure
        // and scale to fit within the icon bounds with
        // padding.
        var refFont = new Font(FONT_FAMILY, Font.BOLD, 200);
        GlyphVector gv = refFont.createGlyphVector(g.getFontRenderContext(), text);
        Rectangle2D bounds = gv.getVisualBounds();

        double available = size - PADDING * 2;
        double scale = Math.min(available / bounds.getWidth(), available / bounds.getHeight());

        double tx = (size - bounds.getWidth() * scale) / 2
            - bounds.getX() * scale;
        double ty = (size - bounds.getHeight() * scale) / 2
            - bounds.getY() * scale;

        var xform = new AffineTransform();
        xform.translate(tx, ty);
        xform.scale(scale, scale);

        g.fill(xform.createTransformedShape(gv.getOutline()));
        g.dispose();

        // Convert BufferedImage -> WritableImage
        var img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                pw.setArgb(px, py, buf.getRGB(px, py));
            }
        }
        return img;
    }

    private AppIcon() {
    }
}
