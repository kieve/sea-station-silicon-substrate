package ca.kieve.ssss.editor;

import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ContentRegistry;

import java.util.HashMap;
import java.util.Map;

public class BlockGlyphResolver {
    private final ContentRegistry m_registry;
    private final Map<String, Character> m_cache = new HashMap<>();

    public BlockGlyphResolver(ContentRegistry registry) {
        m_registry = registry;
    }

    public char resolve(String typeId) {
        return m_cache.computeIfAbsent(typeId, this::lookupGlyph);
    }

    private char lookupGlyph(String typeId) {
        String entityId = "air".equals(typeId) ? "air" : "block_" + typeId;
        if (!m_registry.hasEntity(entityId)) {
            return '?';
        }

        var def = m_registry.getEntityDefinition(entityId);
        var components = def.resolveComponents(m_registry);
        for (ComponentDefinition comp : components) {
            if (comp.type() == TileGlyph.class) {
                Object glyphId = comp.properties().get("glyphId");
                if (glyphId instanceof String id && m_registry.hasGlyph(id)) {
                    return m_registry.getGlyphDefinition(id).character();
                }
            }
        }

        return '?';
    }
}
