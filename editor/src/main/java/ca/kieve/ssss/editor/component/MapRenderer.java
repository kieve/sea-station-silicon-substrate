package ca.kieve.ssss.editor.component;

import java.util.List;
import java.util.Map;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import ca.kieve.ssss.editor.BlockColorResolver;
import ca.kieve.ssss.editor.BlockGlyphResolver;
import ca.kieve.ssss.editor.EditorContext;
import ca.kieve.ssss.editor.EditorTheme;
import ca.kieve.ssss.editor.model.SparseGrid;

/**
 * Game-specific map drawing logic. Renders a SparseGrid
 * to a GraphicsContext using block color/glyph resolution.
 */
public class MapRenderer {
    public static final int CELL_SIZE = 24;
    private static final Font GLYPH_FONT =
            new Font("Consolas", CELL_SIZE * 0.75);
    private static final double MARKER_RATIO = 0.3;

    public record EntityMarker(
            int row, int col, Color color) {}

    private final BlockColorResolver m_colorResolver;
    private final BlockGlyphResolver m_glyphResolver;
    private Map<String, String> m_nameToBpId = Map.of();
    private List<EntityMarker> m_entityMarkers = List.of();

    private SparseGrid m_grid;
    private Integer m_selectedRow;
    private Integer m_selectedCol;
    private Font m_cachedFont;
    private double m_cachedFontZoom;

    public MapRenderer() {
        var ctx = EditorContext.getInstance();
        m_colorResolver = ctx.getColorResolver();
        m_glyphResolver = ctx.getGlyphResolver();
    }

    public void updateNameToBpId(
            Map<String, String> nameToBpId) {
        m_nameToBpId = nameToBpId;
    }

    public void loadEntities(
            List<EntityMarker> markers) {
        m_entityMarkers = markers;
    }

    public void setSelectedCell(
            Integer row, Integer col) {
        m_selectedRow = row;
        m_selectedCol = col;
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

    /** Y pixel offset of the map origin (flipped). */
    public double getMapOriginY() {
        return m_grid != null
                ? -m_grid.getMaxRow() * CELL_SIZE : 0;
    }

    /**
     * Convert a visual row (Y-down screen) back
     * to a data row (Y-up grid index).
     */
    public int visualRowToDataRow(int visualRow) {
        return -visualRow;
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

        if (m_cachedFont == null
                || m_cachedFontZoom != zoom) {
            m_cachedFont = new Font(
                    GLYPH_FONT.getFamily(),
                    CELL_SIZE * 0.75 * zoom);
            m_cachedFontZoom = zoom;
        }
        Font scaledFont = m_cachedFont;

        for (var entry : m_grid.getCells().entrySet()) {
            var pos = entry.getKey();
            String blockName = entry.getValue();

            int flippedRow = -pos.row();
            double x = (pos.col() * CELL_SIZE
                    - cameraX) * zoom;
            double y = (flippedRow * CELL_SIZE
                    - cameraY) * zoom;

            if (x + cell < 0 || x > viewWidth
                    || y + cell < 0
                    || y > viewHeight) {
                continue;
            }

            String bpId = m_nameToBpId.get(blockName);
            Color color;
            char glyph;
            if (bpId != null) {
                color = m_colorResolver.resolve(bpId);
                glyph = m_glyphResolver.resolve(bpId);
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

        double diameter = cell * MARKER_RATIO;
        double radius = diameter / 2.0;
        double pad = cell * 0.08;
        for (var marker : m_entityMarkers) {
            int flippedRow = -marker.row();
            double x = (marker.col() * CELL_SIZE
                    - cameraX) * zoom;
            double y = (flippedRow * CELL_SIZE
                    - cameraY) * zoom;

            if (x + cell < 0 || x > viewWidth
                    || y + cell < 0
                    || y > viewHeight) {
                continue;
            }

            double cx = x + cell - radius - pad;
            double cy = y + radius + pad;

            gc.setFill(Color.gray(0.1, 0.7));
            gc.fillOval(
                    cx - radius - 1,
                    cy - radius - 1,
                    diameter + 2,
                    diameter + 2);

            gc.setFill(marker.color());
            gc.fillOval(
                    cx - radius,
                    cy - radius,
                    diameter,
                    diameter);
        }

        if (m_selectedRow != null
                && m_selectedCol != null) {
            int flippedRow = -m_selectedRow;
            double sx = (m_selectedCol * CELL_SIZE
                    - cameraX) * zoom;
            double sy = (flippedRow * CELL_SIZE
                    - cameraY) * zoom;

            gc.setStroke(EditorTheme.SELECTION_COLOR);
            gc.setLineWidth(2);
            gc.strokeRect(sx, sy, cell, cell);
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
