package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.context.DebugContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.VisionContext;
import ca.kieve.ssss.render.glyphs.CellGlyphComposer;
import ca.kieve.ssss.render.glyphs.CellGlyphSource;
import ca.kieve.ssss.render.layer.RenderFrame;
import ca.kieve.ssss.render.layer.RenderLayer;
import ca.kieve.ssss.render.layer.RenderSurface;

import java.util.List;

public class TileGlyphRenderSystem extends System {
    private final VisionContext m_vision;
    private final DebugContext m_debug;
    private final SpriteBatch m_spriteBatch;
    private final ShapeRenderer m_shapeRenderer;
    private final List<CellGlyphSource> m_sources;
    private final List<RenderLayer> m_layers;
    private final CellGlyphComposer m_composer;

    public TileGlyphRenderSystem(
        GameContext gameContext,
        SpriteBatch spriteBatch,
        ShapeRenderer shapeRenderer,
        List<CellGlyphSource> sources,
        List<RenderLayer> layers
    ) {
        super(gameContext);
        m_vision = gameContext.vision();
        m_debug = gameContext.debug();
        m_spriteBatch = spriteBatch;
        m_shapeRenderer = shapeRenderer;
        m_sources = sources;
        m_layers = layers;
        m_composer = new CellGlyphComposer();
    }

    @Override
    public void run() {
        int cameraZ = m_vision.getCameraZ();
        boolean fullVision = m_debug.isFullVision();

        m_composer.clear();
        for (var source : m_sources) {
            source.collect(cameraZ, m_composer);
        }

        var frame = new RenderFrame(cameraZ, fullVision, m_composer);

        RenderSurface open = null;
        for (var layer : m_layers) {
            if (!layer.shouldRender(frame)) {
                continue;
            }
            var surface = layer.surface();
            if (surface != open) {
                endSurface(open);
                beginSurface(surface);
                open = surface;
            }
            layer.draw(frame);
        }
        endSurface(open);
    }

    private void beginSurface(RenderSurface surface) {
        switch (surface) {
        case SPRITE -> m_spriteBatch.begin();
        case SHAPE_LINE -> m_shapeRenderer.begin(ShapeType.Line);
        }
    }

    private void endSurface(RenderSurface surface) {
        if (surface == null) {
            return;
        }
        switch (surface) {
        case SPRITE -> m_spriteBatch.end();
        case SHAPE_LINE -> m_shapeRenderer.end();
        }
    }
}
