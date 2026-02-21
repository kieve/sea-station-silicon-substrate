package ca.kieve.ssss.editor;

import ca.kieve.ssss.editor.EditorModel.Bounds;
import ca.kieve.ssss.editor.EditorModel.CellKey;

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
import java.util.Map;

public class MapGridPanel extends JPanel {
    private static final int BASE_CELL_SIZE = 24;
    private static final int MIN_CELL_SIZE = 8;
    private static final int MAX_CELL_SIZE = 64;
    private static final int PAN_STEP = 80;
    private static final Color GRID_COLOR = new Color(80, 80, 80);
    private static final Color SPAWN_COLOR = new Color(0, 200, 0, 160);
    private static final Color BACKGROUND_COLOR = new Color(50, 50, 50);

    private static final Map<String, Color> BLOCK_TYPE_COLORS = Map.of(
            "wood", new Color(0x8B, 0x45, 0x13),
            "stone", new Color(0x80, 0x80, 0x80),
            "steel", new Color(0xFF, 0xA5, 0x00),
            "mouse_hole", new Color(0x40, 0x40, 0x40),
            "air", Color.WHITE
    );

    private final EditorModel m_model;
    private int m_cellSize = BASE_CELL_SIZE;
    private int m_panX = 0;
    private int m_panY = 0;
    private Point m_lastPanPoint;
    private boolean m_spawnMode = false;

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
            public void onBlockSelectionChanged() {
            }

            @Override
            public void onDirtyChanged() {
            }
        });
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

        am.put("panLeft", new PanAction(PAN_STEP, 0));
        am.put("panRight", new PanAction(-PAN_STEP, 0));
        am.put("panUp", new PanAction(0, PAN_STEP));
        am.put("panDown", new PanAction(0, -PAN_STEP));
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
                (bounds.maxCol() - bounds.minCol() + 1) * m_cellSize;
        int contentHeight =
                (bounds.maxRow() - bounds.minRow() + 1) * m_cellSize;

        m_panX = (getWidth() - contentWidth) / 2
                - bounds.minCol() * m_cellSize;
        m_panY = (getHeight() - contentHeight) / 2
                - bounds.minRow() * m_cellSize;
        repaint();
    }

    public void setSpawnMode(boolean enabled) {
        m_spawnMode = enabled;
    }

    public boolean isSpawnMode() {
        return m_spawnMode;
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

            int px = m_panX + key.col() * m_cellSize;
            int py = m_panY + key.row() * m_cellSize;

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

        if (m_model.getSpawnZ() == m_model.getActiveLayer()) {
            int px = m_panX + m_model.getSpawnCol() * m_cellSize;
            int py = m_panY + m_model.getSpawnRow() * m_cellSize;
            g2d.setColor(SPAWN_COLOR);
            g2d.fillOval(
                    px + 2, py + 2,
                    m_cellSize - 4, m_cellSize - 4);
            g2d.setColor(Color.GREEN);
            g2d.drawOval(
                    px + 2, py + 2,
                    m_cellSize - 4, m_cellSize - 4);
        }
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(GRID_COLOR);

        int startCol = (-m_panX) / m_cellSize - 1;
        int endCol = (-m_panX + getWidth()) / m_cellSize + 1;
        int startRow = (-m_panY) / m_cellSize - 1;
        int endRow = (-m_panY + getHeight()) / m_cellSize + 1;

        for (int col = startCol; col <= endCol; col++) {
            int x = m_panX + col * m_cellSize;
            g2d.drawLine(x, 0, x, getHeight());
        }
        for (int row = startRow; row <= endRow; row++) {
            int y = m_panY + row * m_cellSize;
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

    private Point screenToGrid(Point screen) {
        int col = Math.floorDiv(screen.x - m_panX, m_cellSize);
        int row = Math.floorDiv(screen.y - m_panY, m_cellSize);
        return new Point(col, row);
    }

    private void paintAtScreen(Point screen) {
        Point grid = screenToGrid(screen);
        if (m_spawnMode) {
            m_model.setPlayerSpawn(grid.x, grid.y);
            m_spawnMode = false;
            return;
        }

        var activeBlock = m_model.getActiveBlock();
        if (activeBlock == null) {
            return;
        }
        m_model.paintCell(
                grid.x, grid.y, activeBlock.layoutChar());
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
                paintAtScreen(e.getPoint());
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
                paintAtScreen(e.getPoint());
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
