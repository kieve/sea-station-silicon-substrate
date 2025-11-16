package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.context.GameContext;

public class ExamineCrosshairRenderSystem extends System {
    private static final float CORNER_LENGTH = 0.25f;

    private final ShapeRenderer m_shapeRenderer;

    public ExamineCrosshairRenderSystem(
        GameContext gameContext,
        ShapeRenderer shapeRenderer
    ) {
        super(gameContext);
        m_shapeRenderer = shapeRenderer;
    }

    @Override
    public void run() {
        var examineContext = m_gameContext.examine();
        if (!examineContext.isActive()) {
            return;
        }

        var pos = examineContext.getCrosshairPos();
        float x = pos.x;
        float y = pos.y;

        m_shapeRenderer.begin(ShapeType.Line);
        m_shapeRenderer.setColor(Color.YELLOW);

        // Top-left corner
        m_shapeRenderer.line(x, y, x + CORNER_LENGTH, y);
        m_shapeRenderer.line(x, y, x, y + CORNER_LENGTH);

        // Top-right corner
        m_shapeRenderer.line(x + 1 - CORNER_LENGTH, y, x + 1, y);
        m_shapeRenderer.line(x + 1, y, x + 1, y + CORNER_LENGTH);

        // Bottom-left corner
        m_shapeRenderer.line(x, y + 1 - CORNER_LENGTH, x, y + 1);
        m_shapeRenderer.line(x, y + 1, x + CORNER_LENGTH, y + 1);

        // Bottom-right corner
        m_shapeRenderer.line(x + 1, y + 1 - CORNER_LENGTH, x + 1, y + 1);
        m_shapeRenderer.line(x + 1 - CORNER_LENGTH, y + 1, x + 1, y + 1);

        m_shapeRenderer.end();
    }
}
