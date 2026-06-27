package ca.kieve.ssss.render;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.context.FluidContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterGlyphsTest {
    @Test
    void glyphIdMapsEachWaveLevelToItsGlyph() {
        assertEquals("water_1", WaterGlyphs.glyphIdForLevel(1));
        assertEquals("water_3", WaterGlyphs.glyphIdForLevel(2));
        assertEquals("water_5", WaterGlyphs.glyphIdForLevel(3));
    }

    @Test
    void glyphIdRejectsEmptyAndFullCells() {
        assertThrows(IllegalArgumentException.class, () -> WaterGlyphs.glyphIdForLevel(0));
        assertThrows(
            IllegalArgumentException.class,
            () -> WaterGlyphs.glyphIdForLevel(FluidContext.MAX_LEVEL)
        );
    }

    @Test
    void deeperWaterIsDarkerAndMoreOpaque() {
        var shallow = WaterGlyphs.colorForLevel(1);
        var deep = WaterGlyphs.colorForLevel(FluidContext.MAX_LEVEL);

        assertTrue(deep.a > shallow.a, "deep water should be more opaque");
        assertTrue(
            deep.r + deep.g + deep.b < shallow.r + shallow.g + shallow.b,
            "deep water should be darker"
        );
    }
}
