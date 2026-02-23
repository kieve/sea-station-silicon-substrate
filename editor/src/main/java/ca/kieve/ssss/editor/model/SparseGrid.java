package ca.kieve.ssss.editor.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A sparse 2D grid of characters that supports arbitrary
 * (including negative) coordinates. Tracks the bounding box
 * of all occupied cells.
 */
public class SparseGrid {
    public static final char EMPTY = ' ';

    public record CellPos(int row, int col) {}

    private final Map<CellPos, Character> m_cells =
            new HashMap<>();
    private int m_minRow;
    private int m_minCol;
    private int m_maxRow;
    private int m_maxCol;

    public char getCell(int row, int col) {
        Character ch = m_cells.get(new CellPos(row, col));
        return ch != null ? ch : EMPTY;
    }

    /**
     * Set a cell value. Space characters remove the cell.
     * Returns true if the grid was modified.
     */
    public boolean setCell(int row, int col, char ch) {
        var pos = new CellPos(row, col);
        if (ch == EMPTY) {
            Character prev = m_cells.remove(pos);
            if (prev == null) {
                return false;
            }
            refreshBounds();
            return true;
        }

        Character prev = m_cells.get(pos);
        if (prev != null && prev == ch) {
            return false;
        }
        m_cells.put(pos, ch);
        expandBounds(row, col);
        return true;
    }

    public Map<CellPos, Character> getCells() {
        return Collections.unmodifiableMap(m_cells);
    }

    public boolean isEmpty() {
        return m_cells.isEmpty();
    }

    public int getMinRow() {
        return m_minRow;
    }

    public int getMinCol() {
        return m_minCol;
    }

    public int getMaxRow() {
        return m_maxRow;
    }

    public int getMaxCol() {
        return m_maxCol;
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

    public void refreshBounds() {
        if (m_cells.isEmpty()) {
            m_minRow = 0;
            m_minCol = 0;
            m_maxRow = 0;
            m_maxCol = 0;
            return;
        }
        m_minRow = Integer.MAX_VALUE;
        m_minCol = Integer.MAX_VALUE;
        m_maxRow = Integer.MIN_VALUE;
        m_maxCol = Integer.MIN_VALUE;
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
