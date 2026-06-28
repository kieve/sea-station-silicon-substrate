package ca.kieve.ssss.render.layer;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import ca.kieve.ssss.context.VisionContext;

public final class GhostLayer implements RenderLayer {
    private static final float GHOST_BRIGHTNESS = 0.3f;

    private final SpriteBatch m_spriteBatch;
    private final VisionContext m_vision;

    public GhostLayer(SpriteBatch spriteBatch, VisionContext vision) {
        m_spriteBatch = spriteBatch;
        m_vision = vision;
    }

    @Override
    public RenderSurface surface() {
        return RenderSurface.SPRITE;
    }

    @Override
    public boolean shouldRender(RenderFrame frame) {
        return !frame.fullVision();
    }

    @Override
    public void draw(RenderFrame frame) {
        for (var ghostEntry : m_vision.getGhostEntries(frame.cameraZ()).entrySet()) {
            var parts = ghostEntry.getKey().split(",");
            int gx = Integer.parseInt(parts[0]);
            int gy = Integer.parseInt(parts[1]);

            if (m_vision.isVisible(gx, gy)) {
                continue;
            }

            var ghost = ghostEntry.getValue();
            ghost.font.setColor(
                ghost.color.r * GHOST_BRIGHTNESS,
                ghost.color.g * GHOST_BRIGHTNESS,
                ghost.color.b * GHOST_BRIGHTNESS,
                ghost.color.a
            );
            ghost.font.draw(m_spriteBatch, "" + ghost.glyph, gx + ghost.dx, gy + ghost.dy);
        }
    }
}
