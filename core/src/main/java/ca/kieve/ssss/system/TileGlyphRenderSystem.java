package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Hidden;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.VisionContext;

import java.util.HashMap;
import java.util.Map;

public class TileGlyphRenderSystem extends System {
    private static final float GHOST_BRIGHTNESS = 0.3f;

    private final Dominion m_ecs;
    private final VisionContext m_vision;
    private final SpriteBatch m_spriteBatch;
    private final ShapeRenderer m_shapeRenderer;
    private final TileGlyph m_floorGlyph;

    private boolean m_debugGrid = false;

    public TileGlyphRenderSystem(
        GameContext gameContext,
        SpriteBatch spriteBatch,
        ShapeRenderer shapeRenderer
    ) {
        super(gameContext);
        m_ecs = gameContext.ecs();
        m_vision = gameContext.vision();
        m_spriteBatch = spriteBatch;
        m_shapeRenderer = shapeRenderer;
        var floorGlyphId = gameContext.mapGenerator().getFloorGlyphId();
        m_floorGlyph = gameContext.entityFactory().getGlyphFactory().getGlyph(floorGlyphId);
    }

    public void setDebugGrid(boolean debugGrid) {
        m_debugGrid = debugGrid;
    }

    @Override
    public void run() {
        int cameraZ = m_vision.getCameraZ();

        var entities = m_ecs.findEntitiesWith(Position.class, TileGlyph.class);

        Map<String, Entity> topEntities = new HashMap<>();
        Map<String, Integer> topZIndex = new HashMap<>();
        Map<String, Integer> topEntityZ = new HashMap<>();

        entities.forEach(with -> {
            var position = with.comp1();
            var pos = position.getPosition();
            var entity = with.entity();

            if (entity.has(Hidden.class)) {
                return;
            }

            int relativeZ = pos.z - cameraZ;
            if (relativeZ < -1 || relativeZ > 0) {
                return;
            }

            var hint = entity.get(RenderingHint.class);
            int zIndex = (hint != null) ? hint.zIndex : 0;

            if (zIndex == -1) {
                return;
            }

            String key = pos.x + "," + pos.y;
            int priority = relativeZ * 100 + zIndex;

            var currentTop = topZIndex.get(key);
            if (currentTop != null && priority <= currentTop) {
                return;
            }
            topEntities.put(key, entity);
            topZIndex.put(key, priority);
            topEntityZ.put(key, pos.z);
        });

        m_spriteBatch.begin();
        for (var entry : topEntities.entrySet()) {
            var key = entry.getKey();
            var entity = entry.getValue();

            var position = entity.get(Position.class);
            var pos = position.getPosition();
            int relativeZ = topEntityZ.get(key) - cameraZ;

            if (!m_vision.isVisible(pos.x, pos.y)) {
                continue;
            }

            var glyph = entity.get(TileGlyph.class);
            if (glyph == null) {
                continue;
            }

            TileGlyph renderGlyph = (relativeZ == -1) ? m_floorGlyph : glyph;

            var font = renderGlyph.font();

            var color = Color.WHITE;
            var colorComp = entity.get(ColorComp.class);
            if (colorComp != null) {
                color = colorComp.color;
            }

            font.setColor(color);

            font.draw(
                m_spriteBatch,
                "" + renderGlyph.glyph(),
                pos.x + renderGlyph.dx(),
                pos.y + renderGlyph.dy()
            );
        }

        for (var ghostEntry : m_vision.getGhostEntries(cameraZ).entrySet()) {
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
        m_spriteBatch.end();

        if (!m_debugGrid) {
            return;
        }
        m_shapeRenderer.begin(ShapeType.Line);
        m_shapeRenderer.setColor(Color.BLUE);

        for (var key : topEntities.keySet()) {
            var parts = key.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            m_shapeRenderer.rect(x, y, 1, 1);
        }

        m_shapeRenderer.end();
    }
}
