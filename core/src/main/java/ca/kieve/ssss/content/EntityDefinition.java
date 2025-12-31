package ca.kieve.ssss.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record EntityDefinition(List<String> parents, List<ComponentDefinition> components) {
    public EntityDefinition {
        parents = parents != null ? parents : List.of();
        components = components != null ? components : List.of();
    }

    public List<Object> instantiateComponents(ContentRegistry registry) {
        List<ComponentDefinition> allComponents = resolveComponents(registry);
        List<Object> result = new ArrayList<>();
        ComponentFactory factory = registry.getComponentFactory();
        for (ComponentDefinition compDef : allComponents) {
            result.add(factory.createComponent(compDef));
        }
        return result;
    }

    public List<ComponentDefinition> resolveComponents(ContentRegistry registry) {
        Map<Class<?>, ComponentDefinition> componentMap = new HashMap<>();

        // First, add parent components in order (later parents override earlier ones)
        for (String parentId : parents) {
            EntityDefinition parentDef = registry.getEntityDefinition(parentId);
            List<ComponentDefinition> parentComponents = parentDef.resolveComponents(registry);
            for (ComponentDefinition compDef : parentComponents) {
                componentMap.put(compDef.type(), compDef);
            }
        }

        // Then, add/override with this entity's components
        for (ComponentDefinition compDef : components) {
            componentMap.put(compDef.type(), compDef);
        }

        return new ArrayList<>(componentMap.values());
    }
}
