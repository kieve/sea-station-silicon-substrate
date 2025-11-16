package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.context.EjectContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;

public class EjectHighlightRenderSystem extends System {
    private static final float CORNER_LENGTH = 0.25f;
    private static final Color VALID_COLOR = Color.GREEN;
    private static final Color INVALID_COLOR = Color.RED;

    private final EjectContext m_ejectContext;
    private final ShapeRenderer m_shapeRenderer;

    public EjectHighlightRenderSystem(
        GameContext gameContext,
        ShapeRenderer shapeRenderer
    ) {
        super(gameContext);
        m_ejectContext = gameContext.eject();
        m_shapeRenderer = shapeRenderer;
    }

    @Override
    public void run() {
        if (!m_ejectContext.isActive()) {
            return;
        }

        var currentPos = m_ejectContext.getCurrentPos();
        var validDirections = m_ejectContext.getValidDirections();

        m_shapeRenderer.begin(ShapeType.Line);

        for (var entry : validDirections.entrySet()) {
            Vec3i direction = entry.getKey();
            boolean isValid = entry.getValue();

            Vec3i targetPos = currentPos.add(direction);
            float x = targetPos.x;
            float y = targetPos.y;

            m_shapeRenderer.setColor(isValid ? VALID_COLOR : INVALID_COLOR);

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
        }

        m_shapeRenderer.end();
    }
}
