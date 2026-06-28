package ca.kieve.ssss.render.layer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.VisionContext;
import ca.kieve.ssss.render.WaterGlyphs;

public final class WaterFillLayer implements RenderLayer {
    private final SpriteBatch m_spriteBatch;
    private final FluidContext m_fluid;
    private final VisionContext m_vision;
    private final Texture m_whitePixel;

    public WaterFillLayer(SpriteBatch spriteBatch, FluidContext fluid, VisionContext vision) {
        m_spriteBatch = spriteBatch;
        m_fluid = fluid;
        m_vision = vision;
        m_whitePixel = createWhitePixel();
    }

    @Override
    public RenderSurface surface() {
        return RenderSurface.SPRITE;
    }

    @Override
    public boolean shouldRender(RenderFrame frame) {
        return true;
    }

    @Override
    public void draw(RenderFrame frame) {
        m_spriteBatch.setColor(WaterGlyphs.colorForLevel(FluidContext.MAX_LEVEL));
        for (var pos : m_fluid.waterCells()) {
            if (m_fluid.getLevel(pos) < FluidContext.MAX_LEVEL) {
                continue;
            }
            int relativeZ = pos.z - frame.cameraZ();
            if (relativeZ < -1 || relativeZ > 0) {
                continue;
            }
            if (!frame.fullVision() && !m_vision.isVisible(pos.x, pos.y)) {
                continue;
            }
            m_spriteBatch.draw(m_whitePixel, pos.x, pos.y, 1f, 1f);
        }
        m_spriteBatch.setColor(Color.WHITE);
    }

    private static Texture createWhitePixel() {
        var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        var texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
}
