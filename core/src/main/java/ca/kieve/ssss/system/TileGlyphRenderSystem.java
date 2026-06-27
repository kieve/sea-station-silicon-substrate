package ca.kieve.ssss.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import dev.dominion.ecs.api.Dominion;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Hidden;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.content.GlyphFactory;
import ca.kieve.ssss.context.DebugContext;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.VisionContext;
import ca.kieve.ssss.render.GlyphColorResolver;
import ca.kieve.ssss.render.WaterGlyphs;
import ca.kieve.ssss.util.Vec3i;

import java.util.HashMap;
import java.util.Map;

public class TileGlyphRenderSystem extends System {
    private record CellRender(TileGlyph glyph, Color color, int priority) {
    }

    private static final float GHOST_BRIGHTNESS = 0.3f;
    private static final int WATER_Z_INDEX = 0;
    private static final float WATER_DEBUG_SCALE = 0.5f;

    private final Dominion m_ecs;
    private final VisionContext m_vision;
    private final FluidContext m_fluid;
    private final DebugContext m_debug;
    private final SpriteBatch m_spriteBatch;
    private final ShapeRenderer m_shapeRenderer;
    private final GlyphFactory m_glyphFactory;
    private final TileGlyph m_floorGlyph;
    private final GlyphColorResolver m_colorResolver;
    private final Texture m_whitePixel;

    public TileGlyphRenderSystem(
        GameContext gameContext,
        SpriteBatch spriteBatch,
        ShapeRenderer shapeRenderer,
        GlyphColorResolver colorResolver
    ) {
        super(gameContext);
        m_ecs = gameContext.ecs();
        m_vision = gameContext.vision();
        m_fluid = gameContext.fluid();
        m_debug = gameContext.debug();
        m_spriteBatch = spriteBatch;
        m_shapeRenderer = shapeRenderer;
        m_colorResolver = colorResolver;
        m_glyphFactory = gameContext.entityFactory().getGlyphFactory();
        m_floorGlyph = m_glyphFactory.getGlyph(gameContext.mapGenerator().getFloorGlyphId());
        m_whitePixel = createWhitePixel();
    }

    private static Texture createWhitePixel() {
        var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        var texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void run() {
        int cameraZ = m_vision.getCameraZ();
        boolean fullVision = m_debug.isFullVision();

        Map<String, CellRender> topCell = new HashMap<>();
        collectEntities(cameraZ, topCell);
        collectWater(cameraZ, topCell);

        m_spriteBatch.begin();
        drawFullWaterFills(cameraZ, fullVision);

        for (var entry : topCell.entrySet()) {
            var parts = entry.getKey().split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            if (!fullVision && !m_vision.isVisible(x, y)) {
                continue;
            }
            var cell = entry.getValue();
            var glyph = cell.glyph();
            var font = glyph.font();
            font.setColor(cell.color());
            font.draw(m_spriteBatch, "" + glyph.glyph(), x + glyph.dx(), y + glyph.dy());
        }

        if (!fullVision) {
            drawGhosts(cameraZ);
        }
        if (m_debug.isShowWaterDepth()) {
            drawWaterDepth(cameraZ, fullVision);
        }
        m_spriteBatch.end();

        drawDebugGrid(topCell);
    }

    private void collectEntities(int cameraZ, Map<String, CellRender> topCell) {
        m_ecs.findEntitiesWith(Position.class, TileGlyph.class).forEach(with -> {
            var entity = with.entity();
            if (entity.has(Hidden.class)) {
                return;
            }
            var pos = with.comp1().getPosition();

            int relativeZ = pos.z - cameraZ;
            if (relativeZ < -1 || relativeZ > 0) {
                return;
            }

            var hint = entity.get(RenderingHint.class);
            int zIndex = (hint != null) ? hint.zIndex : 0;
            if (zIndex == -1) {
                return;
            }

            int priority = relativeZ * 100 + zIndex;
            String key = pos.x + "," + pos.y;
            var current = topCell.get(key);
            if (current != null && priority <= current.priority()) {
                return;
            }

            TileGlyph glyph = (relativeZ == -1) ? m_floorGlyph : with.comp2();
            var colorComp = entity.get(ColorComp.class);
            var base = colorComp != null ? colorComp.color : Color.WHITE;
            Color color = m_colorResolver.resolve(entity, base);

            topCell.put(key, new CellRender(glyph, color, priority));
        });
    }

    private void collectWater(int cameraZ, Map<String, CellRender> topCell) {
        for (var pos : m_fluid.waterCells()) {
            int level = m_fluid.getLevel(pos);
            if (level <= 0 || level >= FluidContext.MAX_LEVEL) {
                continue;
            }

            int relativeZ = pos.z - cameraZ;
            if (relativeZ < -1 || relativeZ > 0) {
                continue;
            }

            int priority = relativeZ * 100 + WATER_Z_INDEX;
            String key = pos.x + "," + pos.y;
            var current = topCell.get(key);
            if (current != null && priority <= current.priority()) {
                continue;
            }

            TileGlyph glyph = m_glyphFactory.getGlyph(WaterGlyphs.glyphIdForLevel(level));
            topCell.put(key, new CellRender(glyph, WaterGlyphs.colorForLevel(level), priority));
        }
    }

    private void drawFullWaterFills(int cameraZ, boolean fullVision) {
        m_spriteBatch.setColor(WaterGlyphs.colorForLevel(FluidContext.MAX_LEVEL));
        for (var pos : m_fluid.waterCells()) {
            if (m_fluid.getLevel(pos) < FluidContext.MAX_LEVEL) {
                continue;
            }
            int relativeZ = pos.z - cameraZ;
            if (relativeZ < -1 || relativeZ > 0) {
                continue;
            }
            if (!fullVision && !m_vision.isVisible(pos.x, pos.y)) {
                continue;
            }
            m_spriteBatch.draw(m_whitePixel, pos.x, pos.y, 1f, 1f);
        }
        m_spriteBatch.setColor(Color.WHITE);
    }

    private void drawGhosts(int cameraZ) {
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
    }

    private void drawWaterDepth(int cameraZ, boolean fullVision) {
        var font = m_floorGlyph.font();
        font.setColor(Color.WHITE);
        float prevScaleX = font.getData().scaleX;
        float prevScaleY = font.getData().scaleY;
        font.getData().setScale(prevScaleX * WATER_DEBUG_SCALE, prevScaleY * WATER_DEBUG_SCALE);
        for (var pos : m_fluid.waterCells()) {
            int relativeZ = pos.z - cameraZ;
            if (relativeZ < -1 || relativeZ > 0) {
                continue;
            }
            if (!fullVision && !m_vision.isVisible(pos.x, pos.y)) {
                continue;
            }
            font.draw(m_spriteBatch, waterDebugLabel(pos), pos.x + 0.05f, pos.y + 0.95f);
        }
        font.getData().setScale(prevScaleX, prevScaleY);
    }

    private String waterDebugLabel(Vec3i pos) {
        if (m_fluid.isSource(pos)) {
            return "S";
        }
        double mass = m_fluid.getMass(pos);
        if (mass >= 10) {
            return Integer.toString((int) Math.round(mass));
        }
        String label = String.format("%.1f", mass);
        if (label.endsWith(".0")) {
            label = label.substring(0, label.length() - 2);
        }
        if (label.startsWith("0") && label.length() > 1) {
            label = label.substring(1);
        }
        return label;
    }

    private void drawDebugGrid(Map<String, CellRender> topCell) {
        if (!m_debug.isDebugGrid()) {
            return;
        }
        m_shapeRenderer.begin(ShapeType.Line);
        m_shapeRenderer.setColor(Color.BLUE);
        for (var key : topCell.keySet()) {
            var parts = key.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            m_shapeRenderer.rect(x, y, 1, 1);
        }
        m_shapeRenderer.end();
    }
}
