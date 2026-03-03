package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Hidden;
import ca.kieve.ssss.component.Player;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.context.GameContext;

import java.util.HashMap;
import java.util.Map;

public class TileGlyphRenderSystem extends System {
    private final SpriteBatch m_spriteBatch;
    private final ShapeRenderer m_shapeRenderer;
    private final TileGlyph m_floorGlyph;

    private boolean m_debugGrid = false;

    public TileGlyphRenderSystem(
        GameContext gameContext,
        SpriteBatch spriteBatch,
        ShapeRenderer shapeRenderer,
        String floorGlyphId
    ) {
        super(gameContext);
        m_spriteBatch = spriteBatch;
        m_shapeRenderer = shapeRenderer;
        m_floorGlyph = gameContext.entityFactory().getGlyphFactory().getGlyph(floorGlyphId);
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

            // Skip hidden entities (e.g., player while socketed into a body)
            if (entity.has(Hidden.class)) {
                return;
            }

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
            if (currentTop != null && priority <= currentTop) {
                return;
            }
            topEntities.put(key, entity);
            topZIndex.put(key, priority);
            topEntityZ.put(key, pos.z);
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

    private int getCameraZ() {
        // Get the camera Z-level from the player's current body
        var playerResults = m_gameContext.ecs().findEntitiesWith(Player.class, CameraComp.class);
        var playerResult = playerResults.stream().findFirst();
        if (playerResult.isEmpty()) {
            return 1;
        }

        var playerEntity = playerResult.get().entity();
        var socketPlug = playerEntity.get(SocketPlug.class);

        // If socketed, use body's position for camera Z
        if (socketPlug != null && socketPlug.currentBody != null) {
            var bodyPos = socketPlug.currentBody.get(Position.class);
            if (bodyPos != null) {
                return bodyPos.getPosition().z;
            }
        }

        // Use player's position for camera Z
        var playerPos = playerEntity.get(Position.class);
        if (playerPos != null) {
            return playerPos.getPosition().z;
        }

        // Default to Z=1 if no position found
        return 1;
    }
}
