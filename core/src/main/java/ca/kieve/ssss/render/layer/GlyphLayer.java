package ca.kieve.ssss.render.layer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ca.kieve.ssss.context.VisionContext;

public final class GlyphLayer implements RenderLayer {
    private final SpriteBatch m_spriteBatch;
    private final VisionContext m_vision;

    public GlyphLayer(SpriteBatch spriteBatch, VisionContext vision) {
        m_spriteBatch = spriteBatch;
        m_vision = vision;
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
        for (var cell : frame.composer().cells()) {
            int x = cell.cell().x;
            int y = cell.cell().y;
            if (!frame.fullVision() && !m_vision.isVisible(x, y)) {
                continue;
            }
            var glyph = cell.glyph();
            var font = glyph.font();
            font.setColor(cell.color());
            font.draw(m_spriteBatch, "" + glyph.glyph(), x + glyph.dx(), y + glyph.dy());
        }
    }
}
