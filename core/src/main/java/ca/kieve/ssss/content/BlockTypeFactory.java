package ca.kieve.ssss.content;

import ca.kieve.ssss.component.Opaque;
import ca.kieve.ssss.component.Solid;

public class BlockTypeFactory {
    private final ContentRegistry m_registry;

    public BlockTypeFactory(ContentRegistry registry) {
        m_registry = registry;
    }

    public boolean isSolid(String typeId) {
        String entityId = getEntityId(typeId);
        if (!m_registry.hasEntity(entityId)) {
            return false;
        }
        EntityDefinition def = m_registry.getEntityDefinition(entityId);
        return hasComponentType(def, Solid.class);
    }

    public boolean isOpaque(String typeId) {
        String entityId = getEntityId(typeId);
        if (!m_registry.hasEntity(entityId)) {
            return false;
        }
        EntityDefinition def = m_registry.getEntityDefinition(entityId);
        return hasComponentType(def, Opaque.class);
    }

    private String getEntityId(String typeId) {
        if ("air".equals(typeId)) {
            return "air";
        }
        return "block_" + typeId;
    }

    private boolean hasComponentType(EntityDefinition def, Class<?> componentType) {
        var components = def.resolveComponents(m_registry);
        for (var comp : components) {
            if (comp.type() == componentType) {
                return true;
            }
        }
        return false;
    }
}
