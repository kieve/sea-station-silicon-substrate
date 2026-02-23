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
    private Map<String, String> m_nameToType = Map.of();

    private SparseGrid m_grid;

    public MapRenderer(BlockColorResolver colorResolver,
                       BlockGlyphResolver glyphResolver) {
        m_colorResolver = colorResolver;
        m_glyphResolver = glyphResolver;
    }

    public void updateNameToType(
            Map<String, String> nameToType) {
        m_nameToType = nameToType;
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
     *
     * @param zoom zoom factor (1.0 = 100%)
     */
    public void render(GraphicsContext gc,
                       double viewWidth,
                       double viewHeight,
                       double cameraX,
                       double cameraY,
                       double zoom) {
        if (m_grid == null) {
            return;
        }

        gc.setFill(EditorTheme.CANVAS_BACKGROUND);
        gc.fillRect(0, 0, viewWidth, viewHeight);

        double cell = CELL_SIZE * zoom;

        drawInfiniteGrid(
                gc, viewWidth, viewHeight,
                cameraX, cameraY, cell);

        Font scaledFont = new Font(
                GLYPH_FONT.getFamily(), CELL_SIZE * 0.75 * zoom);

        for (var entry : m_grid.getCells().entrySet()) {
            var pos = entry.getKey();
            String blockName = entry.getValue();

            double x = (pos.col() * CELL_SIZE - cameraX) * zoom;
            double y = (pos.row() * CELL_SIZE - cameraY) * zoom;

            if (x + cell < 0 || x > viewWidth
                    || y + cell < 0
                    || y > viewHeight) {
                continue;
            }

            String typeId = m_nameToType.get(blockName);
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
            gc.fillRect(x, y, cell, cell);

            gc.setFont(scaledFont);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFill(color);
            gc.fillText(
                    String.valueOf(glyph),
                    x + cell / 2.0,
                    y + cell / 2.0);

            gc.setStroke(EditorTheme.GRID_COLOR);
            gc.setLineWidth(0.5);
            gc.strokeRect(x, y, cell, cell);
        }
    }

    private void drawInfiniteGrid(GraphicsContext gc,
                                  double viewWidth,
                                  double viewHeight,
                                  double cameraX,
                                  double cameraY,
                                  double cell) {
        gc.setStroke(EditorTheme.INFINITE_GRID_COLOR);
        gc.setLineWidth(0.5);

        double scaledCamX = cameraX * (cell / CELL_SIZE);
        double scaledCamY = cameraY * (cell / CELL_SIZE);

        double offsetX =
                -((scaledCamX % cell) + cell) % cell;
        double offsetY =
                -((scaledCamY % cell) + cell) % cell;

        for (double x = offsetX; x <= viewWidth;
                x += cell) {
            gc.strokeLine(x, 0, x, viewHeight);
        }
        for (double y = offsetY; y <= viewHeight;
                y += cell) {
            gc.strokeLine(0, y, viewWidth, y);
        }
    }
}
