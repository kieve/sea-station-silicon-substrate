package ca.kieve.ssss.editor.component;

import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.content.MapDefinition;
import ca.kieve.ssss.editor.BlockColorResolver;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Game-specific map drawing logic. Parses layer data from a MapDefinition
 * and renders visible cells to a GraphicsContext.
 */
public class MapRenderer {
    private static final int CELL_SIZE = 24;
    private static final Color GRID_COLOR = Color.gray(0.85);
    private static final Color BACKGROUND_COLOR = Color.gray(0.2);

    private final BlockColorResolver m_colorResolver;
    private final Map<Character, String> m_charToType = new HashMap<>();

    private int m_mapCols;
    private int m_mapRows;
    private String[] m_currentRows;

    public MapRenderer(BlockColorResolver colorResolver,
                       MapDefinition mapDef) {
        m_colorResolver = colorResolver;

        for (var entry : mapDef.blocks().entrySet()) {
            MapBlockDefinition blockDef = entry.getValue();
            m_charToType.put(blockDef.layoutChar(), blockDef.type());
        }
    }

    /**
     * Parse the layer at the given z-level. Returns true if the layer
     * exists, false otherwise.
     */
    public boolean loadLayer(MapDefinition mapDef, int zLevel) {
        String layerKey = String.valueOf(zLevel);
        String layerText = mapDef.layers().get(layerKey);
        if (layerText == null) {
            return false;
        }

        m_currentRows = layerText.split("\n");
        m_mapRows = m_currentRows.length;
        m_mapCols = 0;
        for (String row : m_currentRows) {
            m_mapCols = Math.max(m_mapCols, row.length());
        }
        return true;
    }

    public int getMapCols() {
        return m_mapCols;
    }

    public int getMapRows() {
        return m_mapRows;
    }

    /** Map width in pixels. */
    public double getMapWidth() {
        return m_mapCols * CELL_SIZE;
    }

    /** Map height in pixels. */
    public double getMapHeight() {
        return m_mapRows * CELL_SIZE;
    }

    /**
     * Render the current layer to the given graphics context.
     */
    public void render(GraphicsContext gc,
                       double viewWidth,
                       double viewHeight,
                       double cameraX,
                       double cameraY) {
        if (m_currentRows == null) {
            return;
        }

        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, viewWidth, viewHeight);

        for (int row = 0; row < m_mapRows; row++) {
            String line = m_currentRows[row];
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                if (ch == ' ') {
                    continue;
                }

                double x = col * CELL_SIZE - cameraX;
                double y = row * CELL_SIZE - cameraY;

                if (x + CELL_SIZE < 0 || x > viewWidth
                        || y + CELL_SIZE < 0 || y > viewHeight) {
                    continue;
                }

                String typeId = m_charToType.get(ch);
                Color color;
                if (typeId != null) {
                    color = m_colorResolver.resolve(typeId);
                } else {
                    color = Color.MAGENTA;
                }

                gc.setFill(color);
                gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                gc.setStroke(GRID_COLOR);
                gc.setLineWidth(0.5);
                gc.strokeRect(x, y, CELL_SIZE, CELL_SIZE);
            }
        }
    }
}
