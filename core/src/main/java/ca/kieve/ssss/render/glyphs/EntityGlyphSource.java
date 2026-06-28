package ca.kieve.ssss.render.glyphs;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Dominion;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Hidden;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.render.GlyphColorResolver;

public final class EntityGlyphSource implements CellGlyphSource {
    private final Dominion m_ecs;
    private final TileGlyph m_floorGlyph;
    private final GlyphColorResolver m_colorResolver;

    public EntityGlyphSource(Dominion ecs, TileGlyph floorGlyph, GlyphColorResolver colorResolver) {
        m_ecs = ecs;
        m_floorGlyph = floorGlyph;
        m_colorResolver = colorResolver;
    }

    @Override
    public void collect(int cameraZ, CellGlyphComposer out) {
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
            TileGlyph glyph = (relativeZ == -1) ? m_floorGlyph : with.comp2();
            var colorComp = entity.get(ColorComp.class);
            var base = colorComp != null ? colorComp.color : Color.WHITE;
            Color color = m_colorResolver.resolve(entity, base);

            out.offer(pos.x, pos.y, priority, glyph, color);
        });
    }
}
