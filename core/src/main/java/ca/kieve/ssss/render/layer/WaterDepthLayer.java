package ca.kieve.ssss.render.layer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.context.DebugContext;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.VisionContext;
import ca.kieve.ssss.util.Vec3i;

public final class WaterDepthLayer implements RenderLayer {
    private static final float WATER_DEBUG_SCALE = 0.5f;

    private final SpriteBatch m_spriteBatch;
    private final FluidContext m_fluid;
    private final VisionContext m_vision;
    private final DebugContext m_debug;
    private final TileGlyph m_floorGlyph;

    public WaterDepthLayer(
        SpriteBatch spriteBatch,
        FluidContext fluid,
        VisionContext vision,
        DebugContext debug,
        TileGlyph floorGlyph
    ) {
        m_spriteBatch = spriteBatch;
        m_fluid = fluid;
        m_vision = vision;
        m_debug = debug;
        m_floorGlyph = floorGlyph;
    }

    @Override
    public RenderSurface surface() {
        return RenderSurface.SPRITE;
    }

    @Override
    public boolean shouldRender(RenderFrame frame) {
        return m_debug.isShowWaterDepth();
    }

    @Override
    public void draw(RenderFrame frame) {
        var font = m_floorGlyph.font();
        font.setColor(Color.WHITE);
        float prevScaleX = font.getData().scaleX;
        float prevScaleY = font.getData().scaleY;
        font.getData().setScale(prevScaleX * WATER_DEBUG_SCALE, prevScaleY * WATER_DEBUG_SCALE);
        for (var pos : m_fluid.waterCells()) {
            int relativeZ = pos.z - frame.cameraZ();
            if (relativeZ < -1 || relativeZ > 0) {
                continue;
            }
            if (!frame.fullVision() && !m_vision.isVisible(pos.x, pos.y)) {
                continue;
            }
            font.draw(m_spriteBatch, label(m_fluid, pos), pos.x + 0.05f, pos.y + 0.95f);
        }
        font.getData().setScale(prevScaleX, prevScaleY);
    }

    static String label(FluidContext fluid, Vec3i pos) {
        if (fluid.isSource(pos)) {
            return "S";
        }
        double mass = fluid.getMass(pos);
        if (mass >= 10) {
            return Integer.toString((int) Math.round(mass));
        }
        String label = String.format("%.1f", mass);
        if (label.endsWith(".0")) {
            label = label.substring(0, label.length() - 2);
        }
        if (label.startsWith("0") && label.length() > 1) {
            label = label.substring(1);
        }
        return label;
    }
}
