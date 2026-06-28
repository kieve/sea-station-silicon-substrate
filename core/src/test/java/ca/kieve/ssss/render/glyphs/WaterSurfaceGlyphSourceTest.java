package ca.kieve.ssss.render.glyphs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.GlyphDefinition;
import ca.kieve.ssss.content.GlyphFactory;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.util.Vec3i;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterSurfaceGlyphSourceTest {
    private FluidContext m_fluid;
    private WaterSurfaceGlyphSource m_source;

    @BeforeEach
    void setUp() {
        var registry = new ContentRegistry();
        registry.registerGlyph("water_1", new GlyphDefinition("f", '~', 0, 0));
        registry.registerGlyph("water_3", new GlyphDefinition("f", '~', 0, 0));
        registry.registerGlyph("water_5", new GlyphDefinition("f", '~', 0, 0));
        var glyphFactory = new GlyphFactory(registry, true);

        m_fluid = new FluidContext();
        m_fluid.init(new Vec3i(4, 4, 2));
        m_source = new WaterSurfaceGlyphSource(m_fluid, glyphFactory);
    }

    private Map<Vec3i, CellGlyph> collect(int cameraZ) {
        var composer = new CellGlyphComposer();
        m_source.collect(cameraZ, composer);
        Map<Vec3i, CellGlyph> map = new HashMap<>();
        for (var cell : composer.cells()) {
            map.put(cell.cell(), cell);
        }
        return map;
    }

    @Test
    void partialWaterEmitsSurfaceGlyph() {
        m_fluid.setLevel(new Vec3i(1, 1, 0), 1);
        var cell = collect(0).get(new Vec3i(1, 1, 0));
        assertTrue(cell != null, "a partial water cell should emit a surface glyph");
        assertEquals(0, cell.priority(), "water on the camera plane is priority 0");
    }

    @Test
    void emptyFluidEmitsNothing() {
        assertTrue(collect(0).isEmpty(), "no water means no surface glyphs");
    }

    @Test
    void fullWaterEmitsNoSurfaceGlyph() {
        m_fluid.setLevel(new Vec3i(1, 1, 0), FluidContext.MAX_LEVEL);
        assertFalse(
            collect(0).containsKey(new Vec3i(1, 1, 0)),
            "full cells are drawn as fills, not surface glyphs"
        );
    }

    @Test
    void waterAboveCameraIsSkipped() {
        m_fluid.setLevel(new Vec3i(1, 1, 1), 1);
        assertTrue(collect(0).isEmpty(), "water above the camera plane should be skipped");
    }

    @Test
    void waterOneLevelBelowEmitsWithNegativePriority() {
        m_fluid.setLevel(new Vec3i(1, 1, 0), 1);
        var cell = collect(1).get(new Vec3i(1, 1, 0));
        assertTrue(cell != null, "water one level below the camera should still render");
        assertEquals(-100, cell.priority(), "relativeZ -1 yields priority -100");
    }
}
