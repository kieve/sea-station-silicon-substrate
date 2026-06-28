package ca.kieve.ssss.render.glyphs;

import com.badlogic.gdx.graphics.Color;

import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.util.Vec3i;

import java.util.HashMap;
import java.util.Map;

public final class CellGlyphComposer {
    private final Map<Vec3i, CellGlyph> m_cells = new HashMap<>();

    public void clear() {
        m_cells.clear();
    }

    public void offer(int x, int y, int priority, TileGlyph glyph, Color color) {
        var key = new Vec3i(x, y, 0);
        var current = m_cells.get(key);
        if (current != null && priority <= current.priority()) {
            return;
        }
        m_cells.put(key, new CellGlyph(key, glyph, color, priority));
    }

    public Iterable<CellGlyph> cells() {
        return m_cells.values();
    }
}
