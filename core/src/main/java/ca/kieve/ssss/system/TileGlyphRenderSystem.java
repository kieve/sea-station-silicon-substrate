package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Material;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.context.GameContext;

public class TileGlyphRenderSystem extends System {
    private final SpriteBatch m_spriteBatch;
    private final ShapeRenderer m_shapeRenderer;

    private boolean m_debugGrid = false;

    public TileGlyphRenderSystem(
        GameContext gameContext,
        SpriteBatch spriteBatch,
        ShapeRenderer shapeRenderer
    ) {
        super(gameContext);
        m_spriteBatch = spriteBatch;
        m_shapeRenderer = shapeRenderer;
    }

    public void setDebugGrid(boolean debugGrid) {
        m_debugGrid = debugGrid;
    }

    @Override
    public void run() {
        var entities = m_gameContext.ecs().findEntitiesWith(Position.class, TileGlyph.class);
        m_spriteBatch.begin();
        entities.forEach(with -> {
            var position = with.comp1();
            var glyph = with.comp2();

            // Get all the other entities at the same position
            var stack = m_gameContext.pos().getAt(position.getPosition());
            if (stack.size() > 1) {
                // TODO: Come up with a better way of sorting stacked entities
                boolean hasDescriptor = with.entity().has(Descriptor.class);
                if (!hasDescriptor) {
                    return;
                }
            }

            var pos = position.getPosition();
            var font = glyph.font();

            var material = with.entity().get(Material.class);
            var color = switch (material) {
                case WOOD -> Color.BROWN;
                case STONE -> Color.GRAY;
                case STEEL -> Color.ORANGE;
                case null -> Color.WHITE;
            };

            var setColor = with.entity().get(Color.class);
            if (setColor != null) {
                color = setColor;
            }

            font.setColor(color);

            font.draw(m_spriteBatch, "" + glyph.glyph(),
                pos.x + glyph.dx(),
                pos.y + glyph.dy());
        });
        m_spriteBatch.end();

        if (m_debugGrid) {
            m_shapeRenderer.begin(ShapeType.Line);
            m_shapeRenderer.setColor(Color.BLUE);

            entities.forEach(with -> {
                var position = with.comp1();
                var pos = position.getPosition();
                m_shapeRenderer.rect(pos.x, pos.y, 1, 1);
            });

            m_shapeRenderer.end();
        }
    }
}
