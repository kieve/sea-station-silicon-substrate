package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Material;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.repository.GlyphRepo;
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
        int cameraZ = getCameraZ();

        var entities = m_gameContext.ecs().findEntitiesWith(Position.class, TileGlyph.class);

        // Group entities by 2D position (x,y), keeping only the one with highest zIndex
        // Only include entities at Z-1, Z, or Z+1 relative to camera
        Map<String, Entity> topEntities = new HashMap<>();
        Map<String, Integer> topZIndex = new HashMap<>();
        Map<String, Integer> topEntityZ = new HashMap<>();

        entities.forEach(with -> {
            var position = with.comp1();
            var pos = position.getPosition();
            var entity = with.entity();

            // Only render entities within camera's Z range
            int relativeZ = pos.z - cameraZ;
            if (relativeZ < -1 || relativeZ > 0) {
                // Skip ceiling blocks (Z+1) and anything outside range
                // We only want to render floor (Z-1) and current level (Z)
                return;
            }

            var hint = entity.get(RenderingHint.class);
            int zIndex = (hint != null) ? hint.zIndex : 0;

            // Skip entities with zIndex = -1 (never draw)
            if (zIndex == -1) {
                return;
            }

            // Use 2D key (x,y) for grouping
            String key = pos.x + "," + pos.y;

            // Keep entity with highest combined priority (relativeZ * 100 + zIndex)
            // This prioritizes current level (Z=0) over floor (Z=-1)
            int priority = relativeZ * 100 + zIndex;
            var currentTop = topZIndex.get(key);
            if (currentTop == null || priority > currentTop) {
                topEntities.put(key, entity);
                topZIndex.put(key, priority);
                topEntityZ.put(key, pos.z);
            }
        });

        // Render only the top entity at each 2D position
        m_spriteBatch.begin();
        for (var entry : topEntities.entrySet()) {
            var key = entry.getKey();
            var entity = entry.getValue();

            var position = entity.get(Position.class);
            var pos = position.getPosition();
            int relativeZ = topEntityZ.get(key) - cameraZ;

            var glyph = entity.get(TileGlyph.class);
            if (glyph == null) {
                continue;
            }

            // Choose glyph based on relative Z-level
            // relativeZ == -1: floor level, use floor glyph
            // relativeZ == 0: current level, use entity's normal glyph
            TileGlyph renderGlyph = (relativeZ == -1) ? GlyphRepo.INTERPUNCT : glyph;

            var font = renderGlyph.font();

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

            font.draw(m_spriteBatch, "" + renderGlyph.glyph(),
                pos.x + renderGlyph.dx(),
                pos.y + renderGlyph.dy());
        }
        m_spriteBatch.end();

        if (m_debugGrid) {
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

    private int getCameraZ() {
        // Get the camera Z-level from the player's current body
        var plugResults = m_gameContext.ecs().findEntitiesWith(
            SocketPlug.class,
            CameraComp.class
        );

        var plugResult = plugResults.stream().findFirst();
        if (plugResult.isPresent()) {
            var socketPlug = plugResult.get().comp1();
            if (socketPlug.currentBody != null) {
                var bodyPos = socketPlug.currentBody.get(Position.class);
                if (bodyPos != null) {
                    return bodyPos.getPosition().z;
                }
            }
        }

        // Default to Z=1 if no player found
        return 1;
    }
}
