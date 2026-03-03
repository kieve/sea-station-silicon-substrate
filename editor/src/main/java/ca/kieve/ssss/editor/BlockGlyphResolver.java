package ca.kieve.ssss.editor;

import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRegistry;

public class BlockGlyphResolver {
    private final ContentRegistry m_registry;

    public BlockGlyphResolver(ContentRegistry registry) {
        m_registry = registry;
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
