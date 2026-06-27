package ca.kieve.ssss.editor;

import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.content.MapBlockDefinition;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.render.WaterGlyphs;

public class BlockGlyphResolver {
    private final ContentRegistry m_registry;

    public BlockGlyphResolver(ContentRegistry registry) {
        m_registry = registry;
    }

    public char resolve(MapBlockDefinition def) {
        if (def.waterDepth() != null && def.waterDepth() > 0) {
            return '█';
        }
        Double fill = def.waterFill();
        if (fill != null && fill > 0) {
            int level = FluidContext.levelForMass(fill);
            if (level >= FluidContext.MAX_LEVEL) {
                return '█';
            }
            String glyphId = WaterGlyphs.glyphIdForLevel(level);
            if (m_registry.hasGlyph(glyphId)) {
                return m_registry.getGlyphDefinition(glyphId).character();
            }
            return '~';
        }
        return resolve(def.bpId());
    }

    public char resolve(String bpId) {
        if (!m_registry.hasEntity(bpId)) {
            return '?';
        }

        var def = m_registry.getEntityDefinition(bpId);
        var components = def.resolveComponents(m_registry);
        for (ComponentDefinition comp : components) {
            if (comp.type() != TileGlyph.class) {
                continue;
            }
            Object glyphId = comp.properties().get("glyphId");
            if (glyphId instanceof String id && m_registry.hasGlyph(id)) {
                return m_registry.getGlyphDefinition(id).character();
            }
        }

        return '?';
    }
}
