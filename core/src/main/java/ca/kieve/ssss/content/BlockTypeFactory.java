package ca.kieve.ssss.content;

import ca.kieve.ssss.component.Opaque;
import ca.kieve.ssss.component.Solid;

public class BlockTypeFactory {
    private final ContentRegistry m_registry;

    public BlockTypeFactory(ContentRegistry registry) {
        m_registry = registry;
    }

    public boolean isSolid(String bpId) {
        if (!m_registry.hasEntity(bpId)) {
            return false;
        }
        EntityDefinition def = m_registry.getEntityDefinition(bpId);
        return hasComponentType(def, Solid.class);
    }

    public boolean isOpaque(String bpId) {
        if (!m_registry.hasEntity(bpId)) {
            return false;
        }
        EntityDefinition def = m_registry.getEntityDefinition(bpId);
        return hasComponentType(def, Opaque.class);
    }

    private boolean hasComponentType(
            EntityDefinition def, Class<?> componentType) {
        var components = def.resolveComponents(m_registry);
        for (var comp : components) {
            if (comp.type() == componentType) {
                return true;
            }
        }
        return false;
    }
}
