package ca.kieve.ssss.editor;

import ca.kieve.ssss.editor.EditorModel.Bounds;
import ca.kieve.ssss.editor.EditorModel.CellKey;
import ca.kieve.ssss.editor.EditorModel.EntityPosition;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapGridPanel extends JPanel {
    public interface TileSelectionListener {
        void onTileSelected(int col, int row, int z);
        void onSelectionCleared();
    }

    private static final int BASE_CELL_SIZE = 24;
    private static final int MIN_CELL_SIZE = 8;
    private static final int MAX_CELL_SIZE = 64;
    private static final int PAN_STEP = 80;
    private static final Color GRID_COLOR = new Color(80, 80, 80);
    private static final Color BACKGROUND_COLOR =
            new Color(50, 50, 50);
    private static final Color SELECTION_COLOR =
            new Color(255, 255, 0, 200);
    private static final Color ENTITY_PLAYER_COLOR =
            new Color(0, 200, 0, 200);
    private static final Color ENTITY_OTHER_COLOR =
            new Color(0, 200, 200, 200);

    private static final Map<String, Color> BLOCK_TYPE_COLORS = Map.of(
            "wood", new Color(0x8B, 0x45, 0x13),
            "stone", new Color(0x80, 0x80, 0x80),
            "steel", new Color(0xFF, 0xA5, 0x00),
            "mouse_hole", new Color(0x40, 0x40, 0x40),
            "air", Color.WHITE
    );

    private final EditorModel m_model;
    private final List<TileSelectionListener> m_selectionListeners =
            new ArrayList<>();
    private int m_cellSize = BASE_CELL_SIZE;
    private int m_panX = 0;
    private int m_panY = 0;
    private Point m_lastPanPoint;

    private int m_selectedCol;
    private int m_selectedRow;
    private boolean m_hasSelection = false;

    public MapGridPanel(EditorModel model) {
        m_model = model;
        setBackground(BACKGROUND_COLOR);
        setFocusable(true);

        var mouseHandler = new GridMouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);

        setupKeyBindings();

        m_model.addListener(new EditorModel.Listener() {
            @Override
            public void onModelChanged() {
                repaint();
            }

            @Override
            public void onLayerChanged() {
                repaint();
            }

            @Override
            public void onBlockSelectionChanged() {}

            @Override
            public void onDirtyChanged() {}

            @Override
            public void onEntitiesChanged() {
                repaint();
            }

            @Override
            public void onToolChanged() {}
        });
    }

    public void addTileSelectionListener(
            TileSelectionListener listener) {
        m_selectionListeners.add(listener);
    }

    public void setSelection(int col, int row) {
        m_selectedCol = col;
        m_selectedRow = row;
        m_hasSelection = true;
        repaint();
        for (var l : m_selectionListeners) {
            l.onTileSelected(col, row, m_model.getActiveLayer());
        }
    }

    public void clearSelection() {
        m_hasSelection = false;
        repaint();
        for (var l : m_selectionListeners) {
            l.onSelectionCleared();
        }
    }

    public boolean hasSelection() {
        return m_hasSelection;
    }

    public int getSelectedCol() {
        return m_selectedCol;
    }

    public int getSelectedRow() {
        return m_selectedRow;
    }

    // --- Coordinate conversion (Y-up grid) ---
    // The game world uses Y-up: higher row = higher on screen.
    // Screen pixels use Y-down: higher pixel Y = lower on screen.
    // panX/panY is the screen pixel position of the grid origin.
    // Cell at (col, row) has its top-left screen pixel at:
    //   px = panX + col * cellSize
    //   py = panY - (row + 1) * cellSize

    private int gridToScreenX(int col) {
        return m_panX + col * m_cellSize;
    }

    private int gridToScreenY(int row) {
        return m_panY - (row + 1) * m_cellSize;
    }

    private Point screenToGrid(Point screen) {
        int col = Math.floorDiv(screen.x - m_panX, m_cellSize);
        int row = -Math.floorDiv(screen.y - m_panY, m_cellSize) - 1;
        return new Point(col, row);
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                "panLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0),
                "panLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
                "panRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0),
                "panRight");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                "panUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0),
                "panUp");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                "panDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0),
                "panDown");

        // W/Up: scroll to see higher rows (north) = decrease panY
        // S/Down: scroll to see lower rows (south) = increase panY
        am.put("panLeft", new PanAction(PAN_STEP, 0));
        am.put("panRight", new PanAction(-PAN_STEP, 0));
        am.put("panUp", new PanAction(0, -PAN_STEP));
        am.put("panDown", new PanAction(0, PAN_STEP));
    }

    public void centerOnContent() {
        Bounds bounds = m_model.getGlobalBounds();
        if (bounds == null) {
            m_panX = getWidth() / 2;
            m_panY = getHeight() / 2;
            repaint();
            return;
        }

        int contentWidth =
                (bounds.maxCol() - bounds.minCol() + 1)
                        * m_cellSize;
        int contentHeight =
                (bounds.maxRow() - bounds.minRow() + 1)
                        * m_cellSize;

        // X: same as before
        m_panX = (getWidth() - contentWidth) / 2
                - bounds.minCol() * m_cellSize;

        // Y-up: top of content on screen = gridToScreenY(maxRow)
        //   = panY - (maxRow + 1) * cellSize
        // We want that at: (screenHeight - contentHeight) / 2
        // So: panY = margin + (maxRow + 1) * cellSize
        m_panY = (getHeight() - contentHeight) / 2
                + (bounds.maxRow() + 1) * m_cellSize;

        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(600, 500);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        var g2d = (Graphics2D) g;
        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2d);

        var cellMap = m_model.getActiveCellMap();
        if (cellMap == null) {
            return;
        }

        Font cellFont = new Font(
                Font.MONOSPACED, Font.BOLD, m_cellSize * 2 / 3);
        g2d.setFont(cellFont);
        FontMetrics fm = g2d.getFontMetrics();

        for (var entry : cellMap.entrySet()) {
            CellKey key = entry.getKey();
            char c = entry.getValue();

            int px = gridToScreenX(key.col());
            int py = gridToScreenY(key.row());

            if (px + m_cellSize < 0 || px > getWidth()
                    || py + m_cellSize < 0 || py > getHeight()) {
                continue;
            }

            Color cellColor = getCellColor(c);
            g2d.setColor(cellColor);
            g2d.fillRect(px, py, m_cellSize, m_cellSize);

            if (m_cellSize >= 16) {
                g2d.setColor(getTextColor(cellColor));
                String ch = String.valueOf(c);
                int tx = px
                        + (m_cellSize - fm.stringWidth(ch)) / 2;
                int ty = py
                        + (m_cellSize + fm.getAscent()
                        - fm.getDescent()) / 2;
                g2d.drawString(ch, tx, ty);
            }
        }

        drawEntityMarkers(g2d);
        drawSelection(g2d);
    }

    private void drawEntityMarkers(Graphics2D g2d) {
        int layer = m_model.getActiveLayer();
        var entities = m_model.getEntities();

        var countMap = new HashMap<CellKey, Integer>();
        var firstIdMap = new HashMap<CellKey, String>();

        for (var entity : entities) {
            EntityPosition pos =
                    m_model.getEntityPosition(entity);
            if (pos == null || pos.z() != layer) {
                continue;
            }
            var key = new CellKey(pos.x(), pos.y());
            countMap.merge(key, 1, Integer::sum);
            firstIdMap.putIfAbsent(key, entity.id());
        }

        int markerSize = Math.max(m_cellSize / 4, 4);

        for (var entry : countMap.entrySet()) {
            CellKey key = entry.getKey();
            int count = entry.getValue();
            String firstId = firstIdMap.get(key);

            int px = gridToScreenX(key.col());
            int py = gridToScreenY(key.row());

            if (px + m_cellSize < 0 || px > getWidth()
                    || py + m_cellSize < 0 || py > getHeight()) {
                continue;
            }

            boolean isPlayer = "player".equals(firstId);
            Color markerColor = isPlayer
                    ? ENTITY_PLAYER_COLOR
                    : ENTITY_OTHER_COLOR;

            // Draw diamond in top-right corner
            int cx = px + m_cellSize - markerSize - 1;
            int cy = py + 1;
            int half = markerSize / 2;

            int[] xPoints = {
                cx + half, cx + markerSize, cx + half, cx
            };
            int[] yPoints = {
                cy, cy + half, cy + markerSize, cy + half
            };

            g2d.setColor(markerColor);
            g2d.fillPolygon(xPoints, yPoints, 4);
            g2d.setColor(markerColor.darker());
            g2d.drawPolygon(xPoints, yPoints, 4);

            if (count > 1 && m_cellSize >= 16) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font(
                        Font.SANS_SERIF, Font.BOLD,
                        Math.max(markerSize - 1, 8)));
                String countStr = String.valueOf(count);
                FontMetrics cfm = g2d.getFontMetrics();
                g2d.drawString(countStr,
                        cx + half
                                - cfm.stringWidth(countStr) / 2,
                        cy + half
                                + cfm.getAscent() / 2 - 1);
            }
        }
    }

    private void drawSelection(Graphics2D g2d) {
        if (!m_hasSelection) {
            return;
        }
        int px = gridToScreenX(m_selectedCol);
        int py = gridToScreenY(m_selectedRow);
        g2d.setColor(SELECTION_COLOR);
        g2d.drawRect(px, py, m_cellSize - 1, m_cellSize - 1);
        g2d.drawRect(px + 1, py + 1,
                m_cellSize - 3, m_cellSize - 3);
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(GRID_COLOR);

        int startCol = (-m_panX) / m_cellSize - 1;
        int endCol = (-m_panX + getWidth()) / m_cellSize + 1;
        // Grid lines are at panY + n * cellSize for all integer n,
        // which is the same set regardless of Y direction.
        int startN = (-m_panY) / m_cellSize - 1;
        int endN = (-m_panY + getHeight()) / m_cellSize + 1;

        for (int col = startCol; col <= endCol; col++) {
            int x = m_panX + col * m_cellSize;
            g2d.drawLine(x, 0, x, getHeight());
        }
        for (int n = startN; n <= endN; n++) {
            int y = m_panY + n * m_cellSize;
            g2d.drawLine(0, y, getWidth(), y);
        }
    }

    private Color getCellColor(char c) {
        String blockName = m_model.getBlockNameForChar(c);
        if (blockName == null) {
            return Color.MAGENTA;
        }
        var blockDef = m_model.getBlocks().get(blockName);
        if (blockDef == null) {
            return Color.MAGENTA;
        }
        Color color = BLOCK_TYPE_COLORS.get(blockDef.type());
        if (color != null) {
            return color;
        }
        return new Color(0xAA, 0xAA, 0xAA);
    }

    private Color getTextColor(Color bg) {
        double luminance = 0.299 * bg.getRed()
                + 0.587 * bg.getGreen()
                + 0.114 * bg.getBlue();
        return luminance > 128 ? Color.BLACK : Color.WHITE;
    }

    private void handleLeftClick(Point screen) {
        Point grid = screenToGrid(screen);
        switch (m_model.getActiveTool()) {
            case PAINT -> {
                var activeBlock = m_model.getActiveBlock();
                if (activeBlock != null) {
                    m_model.paintCell(
                            grid.x, grid.y,
                            activeBlock.layoutChar());
                }
            }
            case SELECT -> setSelection(grid.x, grid.y);
            case ERASE -> m_model.eraseCell(grid.x, grid.y);
        }
    }

    private void eraseAtScreen(Point screen) {
        Point grid = screenToGrid(screen);
        m_model.eraseCell(grid.x, grid.y);
    }

    private class PanAction extends AbstractAction {
        private final int m_dx;
        private final int m_dy;

        PanAction(int dx, int dy) {
            m_dx = dx;
            m_dy = dy;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            m_panX += m_dx;
            m_panY += m_dy;
            repaint();
        }
    }

    private class GridMouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();
            if (SwingUtilities.isMiddleMouseButton(e)) {
                m_lastPanPoint = e.getPoint();
                return;
            }
            if (SwingUtilities.isLeftMouseButton(e)) {
                handleLeftClick(e.getPoint());
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                eraseAtScreen(e.getPoint());
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (SwingUtilities.isMiddleMouseButton(e)) {
                int dx = e.getX() - m_lastPanPoint.x;
                int dy = e.getY() - m_lastPanPoint.y;
                m_panX += dx;
                m_panY += dy;
                m_lastPanPoint = e.getPoint();
                repaint();
                return;
            }
            if (SwingUtilities.isLeftMouseButton(e)) {
                handleLeftClick(e.getPoint());
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                eraseAtScreen(e.getPoint());
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int oldSize = m_cellSize;
            if (e.getWheelRotation() < 0) {
                m_cellSize = Math.min(
                        m_cellSize + 2, MAX_CELL_SIZE);
            } else {
                m_cellSize = Math.max(
                        m_cellSize - 2, MIN_CELL_SIZE);
            }

            if (m_cellSize != oldSize) {
                double scale = (double) m_cellSize / oldSize;
                int mouseX = e.getX();
                int mouseY = e.getY();
                m_panX = (int) (mouseX
                        - scale * (mouseX - m_panX));
                m_panY = (int) (mouseY
                        - scale * (mouseY - m_panY));
                repaint();
            }
        }
    }
}
