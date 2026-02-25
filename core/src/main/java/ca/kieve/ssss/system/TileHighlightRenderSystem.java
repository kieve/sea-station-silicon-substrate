package ca.kieve.ssss.system;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.ui.TileHighlight;
import ca.kieve.ssss.ui.TileHighlightProvider;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import java.util.List;

public class TileHighlightRenderSystem extends System {
    private static final float CORNER_LENGTH = 0.25f;

    private final ShapeRenderer m_shapeRenderer;
    private final List<TileHighlightProvider> m_providers;

    public TileHighlightRenderSystem(
        GameContext gameContext,
        ShapeRenderer shapeRenderer,
        List<TileHighlightProvider> providers
    ) {
        super(gameContext);
        m_shapeRenderer = shapeRenderer;
        m_providers = providers;
    }

    @Override
    public void run() {
        boolean anyActive = false;
        for (var provider : m_providers) {
            if (provider.isHighlightActive()) {
                anyActive = true;
                break;
            }
        }
        if (!anyActive) {
            return;
        }

        m_shapeRenderer.begin(ShapeType.Line);

        for (var provider : m_providers) {
            if (!provider.isHighlightActive()) {
                continue;
            }
            for (var highlight : provider.getHighlights()) {
                drawCornerBrackets(highlight);
            }
        }

        m_shapeRenderer.end();
    }

    private void drawCornerBrackets(TileHighlight highlight) {
        float x = highlight.x();
        float y = highlight.y();

        m_shapeRenderer.setColor(highlight.color());

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
}
