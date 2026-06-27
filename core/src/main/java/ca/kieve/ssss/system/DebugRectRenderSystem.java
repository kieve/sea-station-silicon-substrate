package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.component.DebugRect;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.context.GameContext;

/**
 * Renders debug rectangles for entities with DebugRect component.
 */
public class DebugRectRenderSystem extends System {
    private final ShapeRenderer m_shapeRenderer;

    public DebugRectRenderSystem(GameContext gameContext, ShapeRenderer shapeRenderer) {
        super(gameContext);
        m_shapeRenderer = shapeRenderer;
    }

    @Override
    public void run() {
        var entities = m_gameContext.ecs().findEntitiesWith(Position.class, DebugRect.class);

        m_shapeRenderer.begin(ShapeType.Line);

        entities.forEach(with -> {
            var position = with.comp1();
            var debugRect = with.comp2();
            var pos = position.getPosition();

            m_shapeRenderer.setColor(debugRect.color());
            m_shapeRenderer.rect(pos.x + 0.02f, pos.y + 0.1f, 0.8f, 0.8f);
        });

        m_shapeRenderer.end();
    }
}
