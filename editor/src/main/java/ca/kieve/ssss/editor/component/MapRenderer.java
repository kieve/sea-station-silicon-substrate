package ca.kieve.ssss.editor.component;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import ca.kieve.ssss.editor.BlockColorResolver;
import ca.kieve.ssss.editor.BlockGlyphResolver;
import ca.kieve.ssss.editor.EditorContext;
import ca.kieve.ssss.editor.EditorTheme;
import ca.kieve.ssss.editor.model.ComposedWorld;
import ca.kieve.ssss.editor.model.SparseGrid;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Game-specific map drawing logic. Renders a SparseGrid
 * to a GraphicsContext using block color/glyph resolution.
 */
public class MapRenderer {
    public record EntityMarker(int row, int col, Color color) {
    }

    /**
     * Ghost-outlined submap region in single-map mode. Coordinates are in
     * the parent map's coordinate space (row = Y, col = X) — the renderer
     * applies the Y-flip at draw time.
     */
    public record SubmapGhost(String label, int row, int col, int rows, int cols) {
    }

    /**
     * Position of a named connector inside the current map, drawn in the
     * connector tint at its layer. Coordinates are in map coordinate space.
     */
    public record ConnectorMarker(String id, int row, int col, int z) {
    }

    public static final int CELL_SIZE = 24;

    private static final Font GLYPH_FONT = new Font("Consolas", CELL_SIZE * 0.75);
    private static final double MARKER_RATIO = 0.3;
    private static final double SUBMAP_ALPHA = 0.4;

    private final BlockColorResolver m_colorResolver;
    private final BlockGlyphResolver m_glyphResolver;

    private Map<String, String> m_nameToBpId = Map.of();
    private List<EntityMarker> m_entityMarkers = List.of();
    private List<EntityMarker> m_submapEntityMarkers = List.of();
    private List<ComposedWorld.Cell> m_submapCells = List.of();
    private List<SubmapGhost> m_submapGhosts = List.of();
    private List<ConnectorMarker> m_connectorMarkers = List.of();
    private List<ConnectorMarker> m_submapConnectorMarkers = List.of();
    private ComposedWorld m_composedWorld;
    private int m_composedZ;
    private boolean m_composedMode;

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

    public void updateNameToBpId(Map<String, String> nameToBpId) {
        m_nameToBpId = nameToBpId;
    }

    public void loadEntities(List<EntityMarker> markers) {
        m_entityMarkers = markers;
    }

    public void setSubmapOverlay(List<SubmapGhost> ghosts) {
        m_submapGhosts = ghosts != null ? ghosts : List.of();
    }

    public void setSubmapCells(List<ComposedWorld.Cell> cells) {
        m_submapCells = cells != null ? cells : List.of();
    }

    public void setSubmapEntities(List<EntityMarker> markers) {
        m_submapEntityMarkers = markers != null ? markers : List.of();
    }

    public void setConnectorMarkers(List<ConnectorMarker> markers) {
        m_connectorMarkers = markers != null ? markers : List.of();
    }

    /**
     * Connectors owned by child submaps, translated into world coords.
     * Rendered at submap-ghost alpha so that when two connectors line
     * up (parent's local + child's remote one cell apart in the parent
     * connector's direction) the overlap is visually obvious without
     * needing a separate selection reticle.
     */
    public void setSubmapConnectorMarkers(List<ConnectorMarker> markers) {
        m_submapConnectorMarkers = markers != null ? markers : List.of();
    }

    public void setComposedWorld(ComposedWorld world, int z) {
        m_composedWorld = world;
        m_composedZ = z;
    }

    public void setComposedMode(boolean enabled) {
        m_composedMode = enabled;
    }

    public boolean isComposedMode() {
        return m_composedMode;
    }

    public void setSelectedCell(Integer row, Integer col) {
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
            ? m_grid.getMinCol() * CELL_SIZE
            : 0;
    }

    /** Y pixel offset of the map origin (flipped). */
    public double getMapOriginY() {
        return m_grid != null
            ? -m_grid.getMaxRow() * CELL_SIZE
            : 0;
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
    public void render(
        GraphicsContext gc,
        double viewWidth,
        double viewHeight,
        double cameraX,
        double cameraY,
        double zoom
    ) {
        if (m_grid == null) {
            return;
        }

        gc.setFill(EditorTheme.CANVAS_BACKGROUND);
        gc.fillRect(0, 0, viewWidth, viewHeight);

        double cell = CELL_SIZE * zoom;

        drawInfiniteGrid(gc, viewWidth, viewHeight, cameraX, cameraY, cell);

        if (m_cachedFont == null
            || m_cachedFontZoom != zoom) {
            m_cachedFont = new Font(GLYPH_FONT.getFamily(), CELL_SIZE * 0.75 * zoom);
            m_cachedFontZoom = zoom;
        }
        Font scaledFont = m_cachedFont;

        if (m_composedMode && m_composedWorld != null) {
            renderComposed(gc, scaledFont, viewWidth, viewHeight, cameraX, cameraY, zoom, cell);
            return;
        }

        // Submap cells render under local cells so local edits overlay.
        drawSubmapCells(gc, scaledFont, viewWidth, viewHeight, cameraX, cameraY, zoom, cell);

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
            gc.fillText(String.valueOf(glyph), x + cell / 2.0, y + cell / 2.0);

            gc.setStroke(EditorTheme.GRID_COLOR);
            gc.setLineWidth(0.5);
            gc.strokeRect(x, y, cell, cell);
        }

        double diameter = cell * MARKER_RATIO;
        double radius = diameter / 2.0;
        double pad = cell * 0.08;
        drawEntityMarkers(
            gc,
            m_submapEntityMarkers,
            viewWidth,
            viewHeight,
            cameraX,
            cameraY,
            zoom,
            cell,
            diameter,
            radius,
            pad,
            SUBMAP_ALPHA
        );
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
            gc.fillOval(cx - radius - 1, cy - radius - 1, diameter + 2, diameter + 2);

            gc.setFill(marker.color());
            gc.fillOval(cx - radius, cy - radius, diameter, diameter);
        }

        drawConnectorMarkers(gc, cameraX, cameraY, zoom, cell);
        drawSubmapGhosts(gc, cameraX, cameraY, zoom, cell, scaledFont);

        if (m_selectedRow == null
            || m_selectedCol == null) {
            return;
        }

        int flippedRow = -m_selectedRow;
        double sx = (m_selectedCol * CELL_SIZE
            - cameraX) * zoom;
        double sy = (flippedRow * CELL_SIZE
            - cameraY) * zoom;

        gc.setStroke(EditorTheme.SELECTION_COLOR);
        gc.setLineWidth(2);
        gc.strokeRect(sx, sy, cell, cell);
    }

    private void renderComposed(
        GraphicsContext gc,
        Font scaledFont,
        double viewWidth,
        double viewHeight,
        double cameraX,
        double cameraY,
        double zoom,
        double cell
    ) {
        Set<ComposedWorld.CellPos> overlaps = m_composedWorld.overlapCells();
        for (ComposedWorld.Cell composed : m_composedWorld.cellsAt(m_composedZ)) {
            int flippedRow = -composed.row();
            double x = (composed.col() * CELL_SIZE - cameraX) * zoom;
            double y = (flippedRow * CELL_SIZE - cameraY) * zoom;
            if (x + cell < 0 || x > viewWidth || y + cell < 0 || y > viewHeight) {
                continue;
            }

            Color color = m_colorResolver.resolve(composed.bpId());
            char glyph = m_glyphResolver.resolve(composed.bpId());

            gc.setFill(EditorTheme.CELL_BACKGROUND);
            gc.fillRect(x, y, cell, cell);

            gc.setFont(scaledFont);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFill(color);
            gc.fillText(String.valueOf(glyph), x + cell / 2.0, y + cell / 2.0);

            boolean overlap = overlaps.contains(
                new ComposedWorld.CellPos(composed.row(), composed.col(), composed.z())
            );
            gc.setStroke(overlap ? EditorTheme.OVERLAP_WARNING_COLOR : EditorTheme.GRID_COLOR);
            gc.setLineWidth(overlap ? 1.5 : 0.5);
            gc.strokeRect(x, y, cell, cell);
        }

        drawConnectorMarkers(gc, cameraX, cameraY, zoom, cell);
    }

    private void drawSubmapCells(
        GraphicsContext gc,
        Font scaledFont,
        double viewWidth,
        double viewHeight,
        double cameraX,
        double cameraY,
        double zoom,
        double cell
    ) {
        if (m_submapCells.isEmpty()) {
            return;
        }
        gc.setFont(scaledFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        for (ComposedWorld.Cell sc : m_submapCells) {
            int flippedRow = -sc.row();
            double x = (sc.col() * CELL_SIZE - cameraX) * zoom;
            double y = (flippedRow * CELL_SIZE - cameraY) * zoom;
            if (x + cell < 0 || x > viewWidth || y + cell < 0 || y > viewHeight) {
                continue;
            }

            Color color = m_colorResolver.resolve(sc.bpId());
            char glyph = m_glyphResolver.resolve(sc.bpId());

            gc.setFill(
                Color.rgb(
                    (int) (EditorTheme.CELL_BACKGROUND.getRed() * 255),
                    (int) (EditorTheme.CELL_BACKGROUND.getGreen() * 255),
                    (int) (EditorTheme.CELL_BACKGROUND.getBlue() * 255),
                    SUBMAP_ALPHA
                )
            );
            gc.fillRect(x, y, cell, cell);

            gc.setFill(
                Color.color(color.getRed(), color.getGreen(), color.getBlue(), SUBMAP_ALPHA)
            );
            gc.fillText(String.valueOf(glyph), x + cell / 2.0, y + cell / 2.0);
        }
    }

    private static void drawEntityMarkers(
        GraphicsContext gc,
        List<EntityMarker> markers,
        double viewWidth,
        double viewHeight,
        double cameraX,
        double cameraY,
        double zoom,
        double cell,
        double diameter,
        double radius,
        double pad,
        double alpha
    ) {
        if (markers.isEmpty()) {
            return;
        }
        for (var marker : markers) {
            int flippedRow = -marker.row();
            double x = (marker.col() * CELL_SIZE - cameraX) * zoom;
            double y = (flippedRow * CELL_SIZE - cameraY) * zoom;
            if (x + cell < 0 || x > viewWidth || y + cell < 0 || y > viewHeight) {
                continue;
            }
            double cx = x + cell - radius - pad;
            double cy = y + radius + pad;

            gc.setFill(Color.gray(0.1, 0.7 * alpha));
            gc.fillOval(cx - radius - 1, cy - radius - 1, diameter + 2, diameter + 2);

            Color c = marker.color();
            gc.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            gc.fillOval(cx - radius, cy - radius, diameter, diameter);
        }
    }

    private void drawSubmapGhosts(
        GraphicsContext gc,
        double cameraX,
        double cameraY,
        double zoom,
        double cell,
        Font scaledFont
    ) {
        if (m_submapGhosts.isEmpty()) {
            return;
        }
        gc.setStroke(EditorTheme.SUBMAP_GHOST_COLOR);
        gc.setLineWidth(2);
        gc.setFont(scaledFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.TOP);
        gc.setFill(EditorTheme.SUBMAP_GHOST_COLOR);
        for (SubmapGhost ghost : m_submapGhosts) {
            int flippedTopRow = -(ghost.row() + ghost.rows() - 1);
            double x = (ghost.col() * CELL_SIZE - cameraX) * zoom;
            double y = (flippedTopRow * CELL_SIZE - cameraY) * zoom;
            double w = ghost.cols() * cell;
            double h = ghost.rows() * cell;
            gc.strokeRect(x, y, w, h);
            gc.fillText(ghost.label(), x + 4, y + 4);
        }
    }

    private void drawConnectorMarkers(
        GraphicsContext gc,
        double cameraX,
        double cameraY,
        double zoom,
        double cell
    ) {
        // Submap-owned connectors render first at ghost alpha; the
        // parent map's own connectors then render at full opacity over
        // top. When a parent/child connector pair align (one cell apart
        // in the parent connector's direction), they form a visible
        // pair without needing a selection reticle.
        Color ghostColor = Color.color(
            EditorTheme.CONNECTOR_COLOR.getRed(),
            EditorTheme.CONNECTOR_COLOR.getGreen(),
            EditorTheme.CONNECTOR_COLOR.getBlue(),
            SUBMAP_ALPHA
        );
        gc.setLineWidth(1.5);
        for (ConnectorMarker marker : m_submapConnectorMarkers) {
            int flippedRow = -marker.row();
            double x = (marker.col() * CELL_SIZE - cameraX) * zoom;
            double y = (flippedRow * CELL_SIZE - cameraY) * zoom;
            gc.setStroke(ghostColor);
            gc.strokeRect(x + 1, y + 1, cell - 2, cell - 2);
        }
        for (ConnectorMarker marker : m_connectorMarkers) {
            int flippedRow = -marker.row();
            double x = (marker.col() * CELL_SIZE - cameraX) * zoom;
            double y = (flippedRow * CELL_SIZE - cameraY) * zoom;
            gc.setStroke(EditorTheme.CONNECTOR_COLOR);
            gc.strokeRect(x + 1, y + 1, cell - 2, cell - 2);
        }
    }

    private void drawInfiniteGrid(
        GraphicsContext gc,
        double viewWidth,
        double viewHeight,
        double cameraX,
        double cameraY,
        double cell
    ) {
        gc.setStroke(EditorTheme.INFINITE_GRID_COLOR);
        gc.setLineWidth(0.5);

        double scaledCamX = cameraX * (cell / CELL_SIZE);
        double scaledCamY = cameraY * (cell / CELL_SIZE);

        double offsetX = -((scaledCamX % cell) + cell) % cell;
        double offsetY = -((scaledCamY % cell) + cell) % cell;

        for (double x = offsetX; x <= viewWidth; x += cell) {
            gc.strokeLine(x, 0, x, viewHeight);
        }
        for (double y = offsetY; y <= viewHeight; y += cell) {
            gc.strokeLine(0, y, viewWidth, y);
        }
    }
}
