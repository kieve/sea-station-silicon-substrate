package ca.kieve.ssss.repository;

import static ca.kieve.ssss.ui.widget.GameWindow.TILE_SCALE;
import static com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.DEFAULT_CHARS;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class FontRepo {
    private static final String EXTRA_CHARS = "█";

    private FontRepo() {
        // Do not instantiate
    }

    public static final BitmapFont XIROD_32;
    public static final BitmapFont XIROD_24;
    public static final BitmapFont UBUNTU_32;

    public static final BitmapFont UI_UBUNTU_24;

    static {
        XIROD_32 = loadGameFont("fonts/Xirod.otf", 32);
        XIROD_24 = loadGameFont("fonts/Xirod.otf", 24);
        UBUNTU_32 = loadGameFont("fonts/UbuntuMono-R.ttf", 32);
        UI_UBUNTU_24 = loadUiFont("fonts/UbuntuMono-R.ttf", 24);
    }

    private static BitmapFont loadGameFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = DEFAULT_CHARS + EXTRA_CHARS;
        var result = generator.generateFont(parameter);
        result.getData().setScale(TILE_SCALE);
        result.setUseIntegerPositions(false);
        generator.dispose();
        return result;
    }

    private static BitmapFont loadUiFont(String path, int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        parameter.flip = true;
        var result = generator.generateFont(parameter);
        result.setUseIntegerPositions(false);
        generator.dispose();
        return result;
    }

    public static void setFontColor(BitmapFont font, Color color) {
        font.setColor(color);
    }

    public static void draw(BitmapFont font, Batch batch, String text, float x, float y) {
        font.draw(batch, text, x, y);
    }

    /**
     * Wraps text to fit within a maximum width, with optional indentation for continuation lines.
     *
     * @param font The font to use for measuring text width
     * @param text The text to wrap
     * @param maxWidth The maximum width in pixels
     * @param continuationIndent String to prepend to continuation lines (e.g., "    " for 4 spaces)
     * @return List of wrapped lines
     */
    public static List<String> wrapText(
            BitmapFont font,
            String text,
            float maxWidth,
            String continuationIndent) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        GlyphLayout layout = new GlyphLayout();
        float indentWidth = 0;
        if (continuationIndent != null && !continuationIndent.isEmpty()) {
            layout.setText(font, continuationIndent);
            indentWidth = layout.width;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        boolean isFirstLine = true;

        for (String word : words) {
            if (currentLine.isEmpty()) {
                // Starting a new line
                String prefix = isFirstLine ? "" : continuationIndent;
                String testLine = prefix + word;
                layout.setText(font, testLine);

                if (layout.width > maxWidth && !isFirstLine) {
                    // Word alone with indent is too long, try to fit what we can
                    currentLine.append(prefix).append(word);
                } else {
                    currentLine.append(testLine);
                }
            } else {
                // Adding to existing line
                String testLine = currentLine + " " + word;
                layout.setText(font, testLine);

                if (layout.width > maxWidth) {
                    // Word doesn't fit, start new line
                    lines.add(currentLine.toString());
                    isFirstLine = false;
                    currentLine = new StringBuilder();
                    String prefix = continuationIndent != null ? continuationIndent : "";
                    currentLine.append(prefix).append(word);
                } else {
                    currentLine.append(" ").append(word);
                }
            }
        }

        // Add the last line if not empty
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
