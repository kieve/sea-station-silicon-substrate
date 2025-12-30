package ca.kieve.ssss.content;

import ca.kieve.ssss.ai.behavior.BehaviorDefinition;

import java.util.HashMap;
import java.util.Map;

public class ContentRegistry {
    private final Map<String, EntityDefinition> m_entityDefinitions = new HashMap<>();
    private final Map<String, GlyphDefinition> m_glyphDefinitions = new HashMap<>();
    private final Map<String, FontDefinition> m_fontDefinitions = new HashMap<>();
    private final Map<String, BehaviorDefinition> m_behaviorDefinitions = new HashMap<>();

    private final ComponentFactory m_componentFactory;
    private BlockTypeFactory m_blockTypeFactory;

    public ContentRegistry() {
        m_componentFactory = new ComponentFactory(this);
        m_blockTypeFactory = new BlockTypeFactory(this);
    }

    public BlockTypeFactory getBlockTypeFactory() {
        return m_blockTypeFactory;
    }

    public void registerEntity(String id, EntityDefinition definition) {
        m_entityDefinitions.put(id, definition);
    }

    public void registerGlyph(String id, GlyphDefinition definition) {
        m_glyphDefinitions.put(id, definition);
    }

    public void registerFont(String id, FontDefinition definition) {
        m_fontDefinitions.put(id, definition);
    }

    public void registerBehavior(String id, BehaviorDefinition definition) {
        m_behaviorDefinitions.put(id, definition);
    }

    public EntityDefinition getEntityDefinition(String id) {
        EntityDefinition def = m_entityDefinitions.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Unknown entity: " + id);
        }
        return def;
    }

    public GlyphDefinition getGlyphDefinition(String id) {
        GlyphDefinition def = m_glyphDefinitions.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Unknown glyph: " + id);
        }
        return def;
    }

    public FontDefinition getFontDefinition(String id) {
        FontDefinition def = m_fontDefinitions.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Unknown font: " + id);
        }
        return def;
    }

    public BehaviorDefinition getBehaviorDefinition(String id) {
        BehaviorDefinition def = m_behaviorDefinitions.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Unknown behavior: " + id);
        }
        return def;
    }

    public ComponentFactory getComponentFactory() {
        return m_componentFactory;
    }

    public boolean hasEntity(String id) {
        return m_entityDefinitions.containsKey(id);
    }

    public boolean hasGlyph(String id) {
        return m_glyphDefinitions.containsKey(id);
    }

    public boolean hasFont(String id) {
        return m_fontDefinitions.containsKey(id);
    }

    public boolean hasBehavior(String id) {
        return m_behaviorDefinitions.containsKey(id);
    }
}
