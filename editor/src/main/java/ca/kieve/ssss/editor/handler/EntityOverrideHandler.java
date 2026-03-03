package ca.kieve.ssss.editor.handler;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.ComponentTypeDeserializer;
import ca.kieve.ssss.content.ContentRegistry;
import ca.kieve.ssss.editor.EditorContext;
import ca.kieve.ssss.editor.component.ComponentAddDialog;
import ca.kieve.ssss.editor.model.EditorEntity;
import ca.kieve.ssss.editor.model.EditorMapModel;

import java.util.HashSet;
import java.util.List;

public class EntityOverrideHandler {
    @FunctionalInterface
    public interface EntityViewRefresher {
        void refreshEntityView(EditorEntity entity);
    }

    private final EditorMapModel m_model;

    private EntityViewRefresher m_refresher;

    public EntityOverrideHandler(EditorMapModel model) {
        m_model = model;
    }

    public void setRefresher(EntityViewRefresher refresher) {
        m_refresher = refresher;
    }

    private EditorEntity getEntity(Integer entityIndex) {
        if (entityIndex == null) {
            return null;
        }
        List<EditorEntity> entities = m_model.getEntities();
        if (entityIndex < 0 || entityIndex >= entities.size()) {
            return null;
        }
        return entities.get(entityIndex);
    }

    public void onPropertyEdited(
        Integer entityIndex,
        String componentTypeName,
        String propertyName,
        String newValue
    ) {
        EditorEntity entity = getEntity(entityIndex);
        if (entity == null) {
            return;
        }

        ContentRegistry registry = EditorContext.getInstance().getRegistry();

        // Find existing override on the entity
        ComponentDefinition overrideComp = null;
        for (var comp : entity.components()) {
            if (comp.type().getSimpleName().equals(componentTypeName)) {
                overrideComp = comp;
                break;
            }
        }

        if (overrideComp == null) {
            // Creating override from base component
            if (!registry.hasEntity(entity.id())) {
                return;
            }
            var baseDef = registry.getEntityDefinition(entity.id());
            var resolved = baseDef.resolveComponents(registry);
            ComponentDefinition baseComp = null;
            for (var comp : resolved) {
                if (comp.type().getSimpleName().equals(componentTypeName)) {
                    baseComp = comp;
                    break;
                }
            }
            if (baseComp == null) {
                return;
            }

            // Clone the base component into an override
            overrideComp = new ComponentDefinition(baseComp.type());
            for (var prop : baseComp.properties().entrySet()) {
                overrideComp.setProperty(prop.getKey(), prop.getValue());
            }
        }

        // Parse the new value to match original type
        Object oldValue = overrideComp.properties().get(propertyName);
        Object parsed = parseValue(newValue, oldValue);
        overrideComp.setProperty(propertyName, parsed);
        entity.setComponentOverride(overrideComp);
        m_model.markModified();
        m_refresher.refreshEntityView(entity);
    }

    public void handleOverrideAdded(Integer entityIndex, String componentTypeName) {
        EditorEntity entity = getEntity(entityIndex);
        if (entity == null) {
            return;
        }

        ContentRegistry registry = EditorContext.getInstance().getRegistry();

        // Check if base entity has this component
        ComponentDefinition baseComp = null;
        if (registry.hasEntity(entity.id())) {
            var baseDef = registry.getEntityDefinition(entity.id());
            for (var comp : baseDef.resolveComponents(registry)) {
                if (comp.type().getSimpleName().equals(componentTypeName)) {
                    baseComp = comp;
                    break;
                }
            }
        }

        ComponentDefinition newComp;
        if (baseComp != null) {
            // Clone base component (REPLACED)
            newComp = new ComponentDefinition(baseComp.type());
            for (var prop : baseComp.properties().entrySet()) {
                newComp.setProperty(prop.getKey(), prop.getValue());
            }
        } else {
            // New component not in base (ADDED)
            Class<?> clazz = ComponentTypeDeserializer.resolveType(componentTypeName);
            if (clazz == null) {
                return;
            }
            newComp = new ComponentDefinition(clazz);
        }

        entity.setComponentOverride(newComp);
        m_model.markModified();
        m_refresher.refreshEntityView(entity);
    }

    public void handleOverrideRemoved(Integer entityIndex, String componentTypeName) {
        EditorEntity entity = getEntity(entityIndex);
        if (entity == null) {
            return;
        }

        entity.removeComponent(componentTypeName);
        m_model.markModified();
        m_refresher.refreshEntityView(entity);
    }

    public void handlePropertyReverted(
        Integer entityIndex,
        String componentTypeName,
        String propertyName
    ) {
        EditorEntity entity = getEntity(entityIndex);
        if (entity == null) {
            return;
        }

        ContentRegistry registry = EditorContext.getInstance().getRegistry();

        if (!registry.hasEntity(entity.id())) {
            return;
        }
        var baseDef = registry.getEntityDefinition(entity.id());
        var resolved = baseDef.resolveComponents(registry);

        // Find base component
        ComponentDefinition baseComp = null;
        for (var comp : resolved) {
            if (comp.type().getSimpleName().equals(componentTypeName)) {
                baseComp = comp;
                break;
            }
        }
        if (baseComp == null) {
            return;
        }

        // Find override component on entity
        ComponentDefinition overrideComp = null;
        for (var comp : entity.components()) {
            if (comp.type().getSimpleName().equals(componentTypeName)) {
                overrideComp = comp;
                break;
            }
        }
        if (overrideComp == null) {
            return;
        }

        Object baseValue = baseComp.properties().get(propertyName);
        if (baseValue == null) {
            return;
        }
        overrideComp.setProperty(propertyName, baseValue);

        // Check if all properties now match base
        // — if so, remove the override entirely
        boolean allMatch = true;
        for (var entry : overrideComp.properties().entrySet()) {
            Object bv = baseComp.properties().get(entry.getKey());
            if (bv == null || !String.valueOf(bv).equals(String.valueOf(entry.getValue()))) {
                allMatch = false;
                break;
            }
        }
        if (allMatch) {
            entity.removeComponent(componentTypeName);
        }

        m_model.markModified();
        m_refresher.refreshEntityView(entity);
    }

    public void showAddOverrideDialog(Integer entityIndex) {
        EditorEntity entity = getEntity(entityIndex);
        if (entity == null) {
            return;
        }

        ContentRegistry registry = EditorContext.getInstance().getRegistry();

        // Build set of types already present
        var excludeTypes = new HashSet<String>();
        for (var comp : entity.components()) {
            excludeTypes.add(comp.type().getSimpleName());
        }
        if (registry.hasEntity(entity.id())) {
            var baseDef = registry.getEntityDefinition(entity.id());
            for (var comp : baseDef.resolveComponents(registry)) {
                excludeTypes.add(comp.type().getSimpleName());
            }
        }

        var result = ComponentAddDialog.showAdd(excludeTypes);
        result.ifPresent(r -> handleOverrideAdded(entityIndex, r.componentTypeName()));
    }

    static Object parseValue(String value, Object original) {
        if (original instanceof Integer) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return original;
            }
        }
        if (original instanceof Long) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return original;
            }
        }
        if (original instanceof Double) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return original;
            }
        }
        if (original instanceof Float) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return original;
            }
        }
        if (original instanceof Boolean) {
            return Boolean.parseBoolean(value);
        }
        return value;
    }
}
