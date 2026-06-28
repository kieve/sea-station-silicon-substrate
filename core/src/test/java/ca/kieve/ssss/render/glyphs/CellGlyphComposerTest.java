package ca.kieve.ssss.render.glyphs;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.util.Vec3i;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellGlyphComposerTest {
    private final CellGlyphComposer m_composer = new CellGlyphComposer();
    private final TileGlyph m_low = new TileGlyph(null, 'a', 0f, 0f);
    private final TileGlyph m_high = new TileGlyph(null, 'b', 0f, 0f);

    private Map<Vec3i, CellGlyph> snapshot() {
        Map<Vec3i, CellGlyph> map = new HashMap<>();
        for (var cell : m_composer.cells()) {
            map.put(cell.cell(), cell);
        }
        return map;
    }

    @Test
    void higherPriorityReplacesLower() {
        m_composer.offer(1, 1, 0, m_low, Color.WHITE);
        m_composer.offer(1, 1, 5, m_high, Color.RED);

        var cell = snapshot().get(new Vec3i(1, 1, 0));
        assertSame(m_high, cell.glyph(), "higher priority glyph should win the cell");
        assertEquals(5, cell.priority());
    }

    @Test
    void equalPriorityKeepsIncumbent() {
        m_composer.offer(2, 3, 4, m_low, Color.WHITE);
        m_composer.offer(2, 3, 4, m_high, Color.RED);

        var cell = snapshot().get(new Vec3i(2, 3, 0));
        assertSame(m_low, cell.glyph(), "at equal priority the first offer should be kept");
    }

    @Test
    void lowerPriorityIsIgnored() {
        m_composer.offer(0, 0, 10, m_high, Color.RED);
        m_composer.offer(0, 0, 1, m_low, Color.WHITE);

        var cell = snapshot().get(new Vec3i(0, 0, 0));
        assertSame(
            m_high,
            cell.glyph(),
            "a lower priority offer should not displace the incumbent"
        );
    }

    @Test
    void distinctCellsCoexist() {
        m_composer.offer(1, 1, 0, m_low, Color.WHITE);
        m_composer.offer(2, 2, 0, m_high, Color.RED);

        var snapshot = snapshot();
        assertEquals(2, snapshot.size());
        assertSame(m_low, snapshot.get(new Vec3i(1, 1, 0)).glyph());
        assertSame(m_high, snapshot.get(new Vec3i(2, 2, 0)).glyph());
    }

    @Test
    void cellKeyPinsZToZero() {
        m_composer.offer(1, 1, 0, m_low, Color.WHITE);
        var cell = snapshot().keySet().iterator().next();
        assertEquals(0, cell.z, "composer keys pin z to 0 so they act as 2D cell keys");
    }

    @Test
    void clearEmptiesComposer() {
        m_composer.offer(1, 1, 0, m_low, Color.WHITE);
        m_composer.clear();
        assertTrue(snapshot().isEmpty(), "clear should remove all composed cells");
    }
}
