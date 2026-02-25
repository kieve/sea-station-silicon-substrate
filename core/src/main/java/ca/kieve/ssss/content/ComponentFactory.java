package ca.kieve.ssss.content;

import ca.kieve.ssss.ai.behavior.AiController;
import ca.kieve.ssss.ai.behavior.BehaviorDefinition;
import ca.kieve.ssss.component.Equipment;
import ca.kieve.ssss.component.Material;
import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.context.GameContext;

import dev.dominion.ecs.api.Entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Map;

public class ComponentFactory {
    private final ContentRegistry m_registry;
    private GameContext m_gameContext;

    public ComponentFactory(ContentRegistry registry) {
        m_registry = registry;
    }

    public void setGameContext(GameContext gameContext) {
        m_gameContext = gameContext;
    }

    public Object createComponent(ComponentDefinition definition) {
        try {
            Class<?> componentType = definition.type();
            Map<String, Object> properties = definition.properties();

            // Handle special component types that need content lookup
            if (componentType == TileGlyph.class) {
                return createTileGlyph(properties);
            }

            if (componentType == Equipment.class) {
                return createEquipment(properties);
            }

            if (componentType == Material.class) {
                return createMaterial(properties);
            }

            if (componentType == AiController.class) {
                return createAiController(properties);
            }

            // Handle enum components
            if (componentType.isEnum()) {
                return createEnumComponent(componentType, properties);
            }

            if (properties == null || properties.isEmpty()) {
                return createNoArgComponent(componentType);
            }

            return createWithProperties(componentType, properties);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create component: " + definition.type().getSimpleName(), e);
        }
    }

    private TileGlyph createTileGlyph(Map<String, Object> properties) {
        if (properties == null || !properties.containsKey("glyphId")) {
            throw new IllegalArgumentException(
                "TileGlyph component requires glyphId property");
        }

        if (m_gameContext == null) {
            throw new IllegalStateException(
                "GameContext must be set before creating TileGlyph");
        }

        String glyphId = properties.get("glyphId").toString();
        GlyphFactory glyphFactory = m_gameContext.entityFactory().getGlyphFactory();
        return glyphFactory.getGlyph(glyphId);
    }

    private Equipment createEquipment(Map<String, Object> properties) {
        if (properties == null || !properties.containsKey("weaponId")) {
            return new Equipment();
        }

        if (m_gameContext == null) {
            throw new IllegalStateException(
                "GameContext must be set before creating Equipment with weaponId");
        }

        String weaponId = properties.get("weaponId").toString();
        EntityDefinition weaponDef = m_registry.getEntityDefinition(weaponId);

        var components = weaponDef.instantiateComponents(m_registry);
        Entity weaponEntity = m_gameContext.ecs().createEntity(components.toArray());

        return new Equipment(weaponEntity);
    }

    private Material createMaterial(Map<String, Object> properties) {
        if (properties == null || !properties.containsKey("id")) {
            throw new IllegalArgumentException("Material component requires id property");
        }

        if (m_gameContext == null) {
            throw new IllegalStateException(
                "GameContext must be set before creating Material");
        }

        String materialId = properties.get("id").toString();
        EntityDefinition materialDef = m_registry.getEntityDefinition(materialId);

        var components = materialDef.instantiateComponents(m_registry);
        Entity materialEntity = m_gameContext.ecs().createEntity(components.toArray());

        return new Material(materialEntity);
    }

    private AiController createAiController(Map<String, Object> properties) {
        if (properties == null || !properties.containsKey("behavior")) {
            return new AiController();
        }

        String behaviorId = properties.get("behavior").toString();
        BehaviorDefinition behaviorDef = m_registry.getBehaviorDefinition(behaviorId);
        if (behaviorDef == null) {
            throw new IllegalArgumentException(
                "Unknown behavior: " + behaviorId);
        }

        return new AiController(behaviorDef.states());
    }

    @SuppressWarnings("unchecked")
    private Object createEnumComponent(Class<?> enumType, Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            throw new IllegalArgumentException(
                "Enum component " + enumType.getSimpleName() + " requires a value property");
        }

        // Support either "value" property or single property with any name
        String enumValue;
        if (properties.containsKey("value")) {
            enumValue = properties.get("value").toString();
        } else if (properties.size() == 1) {
            enumValue = properties.values().iterator().next().toString();
        } else {
            throw new IllegalArgumentException(
                "Enum component " + enumType.getSimpleName()
                    + " requires 'value' property or single property");
        }

        return Enum.valueOf((Class<Enum>) enumType, enumValue);
    }

    private Object createNoArgComponent(Class<?> componentClass) throws Exception {
        Constructor<?> constructor = componentClass.getDeclaredConstructor();
        return constructor.newInstance();
    }

    private Object createWithProperties(
        Class<?> componentClass,
        Map<String, Object> properties
    ) throws Exception {
        Constructor<?>[] constructors = componentClass.getConstructors();

        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == properties.size()) {
                Object[] convertedArgs = convertProperties(constructor, properties);
                if (convertedArgs != null) {
                    return constructor.newInstance(convertedArgs);
                }
            }
        }

        throw new IllegalArgumentException(
            "No suitable constructor found for " + componentClass.getSimpleName()
                + " with properties: " + properties.keySet());
    }

    private Object[] convertProperties(
        Constructor<?> constructor,
        Map<String, Object> properties
    ) {
        Parameter[] params = constructor.getParameters();
        Object[] converted = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            String paramName = param.getName();
            Class<?> targetType = param.getType();

            if (!properties.containsKey(paramName)) {
                return null;
            }

            Object value = properties.get(paramName);

            try {
                converted[i] = PropertyParser.parseValue(value, targetType);
            } catch (Exception e) {
                return null;
            }
        }

        return converted;
    }
}
