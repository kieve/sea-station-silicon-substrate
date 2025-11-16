package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Material;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;

import dev.dominion.ecs.api.Entity;

import java.util.HashMap;
import java.util.Map;

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

        // Group entities by position, keeping only the one with highest zIndex
        Map<Vec3i, Entity> topEntities = new HashMap<>();
        Map<Vec3i, Integer> topZIndex = new HashMap<>();

        entities.forEach(with -> {
            var position = with.comp1();
            var pos = position.getPosition();
            var entity = with.entity();

            var hint = entity.get(RenderingHint.class);
            int zIndex = (hint != null) ? hint.zIndex : 0;

            // Skip entities with zIndex = -1 (never draw)
            if (zIndex == -1) {
                return;
            }

            // Keep entity with highest zIndex at each position
            var currentTop = topZIndex.get(pos);
            if (currentTop == null || zIndex > currentTop) {
                topEntities.put(pos, entity);
                topZIndex.put(pos, zIndex);
            }
        });

        // Render only the top entity at each position
        m_spriteBatch.begin();
        for (var entry : topEntities.entrySet()) {
            var pos = entry.getKey();
            var entity = entry.getValue();

            var glyph = entity.get(TileGlyph.class);
            if (glyph == null) {
                continue;
            }

            var font = glyph.font();

            var material = entity.get(Material.class);
            var color = switch (material) {
                case WOOD -> Color.BROWN;
                case STONE -> Color.GRAY;
                case STEEL -> Color.ORANGE;
                case null -> Color.WHITE;
            };

            var colorComp = entity.get(ColorComp.class);
            if (colorComp != null) {
                color = colorComp.color;
            }

            font.setColor(color);

            font.draw(m_spriteBatch, "" + glyph.glyph(),
                pos.x + glyph.dx(),
                pos.y + glyph.dy());
        }
        m_spriteBatch.end();

        if (m_debugGrid) {
            m_shapeRenderer.begin(ShapeType.Line);
            m_shapeRenderer.setColor(Color.BLUE);

            for (var pos : topEntities.keySet()) {
                m_shapeRenderer.rect(pos.x, pos.y, 1, 1);
            }

            m_shapeRenderer.end();
        }
    }
}
