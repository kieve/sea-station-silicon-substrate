package ca.kieve.ssss.render.glyphs;

import ca.kieve.ssss.content.GlyphFactory;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.render.WaterGlyphs;

public final class WaterSurfaceGlyphSource implements CellGlyphSource {
    private static final int WATER_Z_INDEX = 0;

    private final FluidContext m_fluid;
    private final GlyphFactory m_glyphFactory;

    public WaterSurfaceGlyphSource(FluidContext fluid, GlyphFactory glyphFactory) {
        m_fluid = fluid;
        m_glyphFactory = glyphFactory;
    }

    @Override
    public void collect(int cameraZ, CellGlyphComposer out) {
        for (var pos : m_fluid.waterCells()) {
            int level = m_fluid.getLevel(pos);
            if (level <= 0 || level >= FluidContext.MAX_LEVEL) {
                continue;
            }

            int relativeZ = pos.z - cameraZ;
            if (relativeZ < -1 || relativeZ > 0) {
                continue;
            }

            int priority = relativeZ * 100 + WATER_Z_INDEX;
            out.offer(
                pos.x,
                pos.y,
                priority,
                m_glyphFactory.getGlyph(WaterGlyphs.glyphIdForLevel(level)),
                WaterGlyphs.colorForLevel(level)
            );
        }
    }
}
