package ca.kieve.ssss.render.layer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import ca.kieve.ssss.context.DebugContext;

public final class DebugGridLayer implements RenderLayer {
    private final ShapeRenderer m_shapeRenderer;
    private final DebugContext m_debug;

    public DebugGridLayer(ShapeRenderer shapeRenderer, DebugContext debug) {
        m_shapeRenderer = shapeRenderer;
        m_debug = debug;
    }

    @Override
    public RenderSurface surface() {
        return RenderSurface.SHAPE_LINE;
    }

    @Override
    public boolean shouldRender(RenderFrame frame) {
        return m_debug.isDebugGrid();
    }

    @Override
    public void draw(RenderFrame frame) {
        m_shapeRenderer.setColor(Color.BLUE);
        for (var cell : frame.composer().cells()) {
            m_shapeRenderer.rect(cell.cell().x, cell.cell().y, 1, 1);
        }
    }
}
