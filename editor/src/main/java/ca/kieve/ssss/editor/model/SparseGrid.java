package ca.kieve.ssss.editor.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A sparse 2D grid of strings (block names) that supports
 * arbitrary (including negative) coordinates. Tracks the
 * bounding box of all occupied cells.
 */
public class SparseGrid {
    public record CellPos(int row, int col) {}

    private final Map<CellPos, String> m_cells =
            new HashMap<>();
    private int m_minRow = Integer.MAX_VALUE;
    private int m_minCol = Integer.MAX_VALUE;
    private int m_maxRow = Integer.MIN_VALUE;
    private int m_maxCol = Integer.MIN_VALUE;

    public String getCell(int row, int col) {
        return m_cells.get(new CellPos(row, col));
    }

    /**
     * Set a cell value. Null removes the cell.
     * Returns true if the grid was modified.
     */
    public boolean setCell(int row, int col, String value) {
        var pos = new CellPos(row, col);
        if (value == null) {
            String prev = m_cells.remove(pos);
            if (prev == null) {
                return false;
            }
            refreshBounds();
            return true;
        }

        String prev = m_cells.get(pos);
        if (value.equals(prev)) {
            return false;
        }
        m_cells.put(pos, value);
        expandBounds(row, col);
        return true;
    }

    public Map<CellPos, String> getCells() {
        return Collections.unmodifiableMap(m_cells);
    }

    public boolean isEmpty() {
        return m_cells.isEmpty();
    }

    public int getMinRow() {
        return m_cells.isEmpty() ? 0 : m_minRow;
    }

    public int getMinCol() {
        return m_cells.isEmpty() ? 0 : m_minCol;
    }

    public int getMaxRow() {
        return m_cells.isEmpty() ? 0 : m_maxRow;
    }

    public int getMaxCol() {
        return m_cells.isEmpty() ? 0 : m_maxCol;
    }

    public int getRows() {
        if (m_cells.isEmpty()) {
            return 0;
        }
        return m_maxRow - m_minRow + 1;
    }

    public int getCols() {
        if (m_cells.isEmpty()) {
            return 0;
        }
        return m_maxCol - m_minCol + 1;
    }

    public void replaceValue(String oldValue, String newValue) {
        for (var entry : m_cells.entrySet()) {
            if (entry.getValue().equals(oldValue)) {
                entry.setValue(newValue);
            }
        }
    }

    public void removeValue(String value) {
        m_cells.values().removeIf(v -> v.equals(value));
        refreshBounds();
    }

    public void refreshBounds() {
        m_minRow = Integer.MAX_VALUE;
        m_minCol = Integer.MAX_VALUE;
        m_maxRow = Integer.MIN_VALUE;
        m_maxCol = Integer.MIN_VALUE;
        if (m_cells.isEmpty()) {
            return;
        }
        for (var pos : m_cells.keySet()) {
            expandBounds(pos.row(), pos.col());
        }
    }

    private void expandBounds(int row, int col) {
        m_minRow = Math.min(m_minRow, row);
        m_maxRow = Math.max(m_maxRow, row);
        m_minCol = Math.min(m_minCol, col);
        m_maxCol = Math.max(m_maxCol, col);
    }
}
