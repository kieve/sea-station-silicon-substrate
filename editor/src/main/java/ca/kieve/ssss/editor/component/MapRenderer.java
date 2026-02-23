package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.editor.BlockColorResolver;
import ca.kieve.ssss.editor.BlockGlyphResolver;
import ca.kieve.ssss.editor.EditorTheme;
import ca.kieve.ssss.editor.model.SparseGrid;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Map;

/**
 * Game-specific map drawing logic. Renders a SparseGrid
 * to a GraphicsContext using block color/glyph resolution.
 */
public class MapRenderer {
    public static final int CELL_SIZE = 24;
    private static final Font GLYPH_FONT =
            new Font("Consolas", CELL_SIZE * 0.75);

    private final BlockColorResolver m_colorResolver;
    private final BlockGlyphResolver m_glyphResolver;
    private Map<Character, String> m_charToType = Map.of();

    private SparseGrid m_grid;

    public MapRenderer(BlockColorResolver colorResolver,
                       BlockGlyphResolver glyphResolver) {
        m_colorResolver = colorResolver;
        m_glyphResolver = glyphResolver;
    }

    public void updateCharToType(
            Map<Character, String> charToType) {
        m_charToType = charToType;
    }

    /**
     * Load a sparse grid for rendering.
     * Returns true if the grid is non-null.
     */
    public boolean loadLayer(SparseGrid grid) {
        if (grid == null) {
            return false;
        }
        m_grid = grid;
        return true;
    }

    public int getMapCols() {
        return m_grid != null ? m_grid.getCols() : 0;
    }

    public int getMapRows() {
        return m_grid != null ? m_grid.getRows() : 0;
    }

    /** Map width in pixels. */
    public double getMapWidth() {
        return getMapCols() * CELL_SIZE;
    }

    /** Map height in pixels. */
    public double getMapHeight() {
        return getMapRows() * CELL_SIZE;
    }

    /** X pixel offset of the map origin (minCol). */
    public double getMapOriginX() {
        return m_grid != null
                ? m_grid.getMinCol() * CELL_SIZE : 0;
    }

    /** Y pixel offset of the map origin (minRow). */
    public double getMapOriginY() {
        return m_grid != null
                ? m_grid.getMinRow() * CELL_SIZE : 0;
    }

    /**
     * Render the current layer to the given graphics context.
     */
    public void render(GraphicsContext gc,
                       double viewWidth,
                       double viewHeight,
                       double cameraX,
                       double cameraY) {
        if (m_grid == null) {
            return;
        }

        gc.setFill(EditorTheme.CANVAS_BACKGROUND);
        gc.fillRect(0, 0, viewWidth, viewHeight);

        drawInfiniteGrid(
                gc, viewWidth, viewHeight, cameraX, cameraY);

        for (var entry : m_grid.getCells().entrySet()) {
            var pos = entry.getKey();
            char ch = entry.getValue();

            double x = pos.col() * CELL_SIZE - cameraX;
            double y = pos.row() * CELL_SIZE - cameraY;

            if (x + CELL_SIZE < 0 || x > viewWidth
                    || y + CELL_SIZE < 0
                    || y > viewHeight) {
                continue;
            }

            String typeId = m_charToType.get(ch);
            Color color;
            char glyph;
            if (typeId != null) {
                color = m_colorResolver.resolve(typeId);
                glyph = m_glyphResolver.resolve(typeId);
            } else {
                color = Color.MAGENTA;
                glyph = '?';
            }

            gc.setFill(EditorTheme.CELL_BACKGROUND);
            gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);

            gc.setFont(GLYPH_FONT);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFill(color);
            gc.fillText(
                    String.valueOf(glyph),
                    x + CELL_SIZE / 2.0,
                    y + CELL_SIZE / 2.0);

            gc.setStroke(EditorTheme.GRID_COLOR);
            gc.setLineWidth(0.5);
            gc.strokeRect(x, y, CELL_SIZE, CELL_SIZE);
        }
    }

    private void drawInfiniteGrid(GraphicsContext gc,
                                  double viewWidth,
                                  double viewHeight,
                                  double cameraX,
                                  double cameraY) {
        gc.setStroke(EditorTheme.INFINITE_GRID_COLOR);
        gc.setLineWidth(0.5);

        double offsetX =
                -((cameraX % CELL_SIZE) + CELL_SIZE)
                        % CELL_SIZE;
        double offsetY =
                -((cameraY % CELL_SIZE) + CELL_SIZE)
                        % CELL_SIZE;

        for (double x = offsetX; x <= viewWidth;
                x += CELL_SIZE) {
            gc.strokeLine(x, 0, x, viewHeight);
        }
        for (double y = offsetY; y <= viewHeight;
                y += CELL_SIZE) {
            gc.strokeLine(0, y, viewWidth, y);
        }
    }
}
