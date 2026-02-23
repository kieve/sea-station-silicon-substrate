package ca.kieve.ssss.content;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class EntityFactory {
    private final ContentRegistry m_registry;
    private final GlyphFactory m_glyphFactory;

    public EntityFactory(ContentRegistry registry) {
        m_registry = registry;
        m_glyphFactory = new GlyphFactory(registry);
    }

    public Entity createEntity(GameContext context, String entityId, Vec3i pos) {
        return createEntity(context, entityId, pos, null);
    }

    public Entity createEntity(GameContext context, String entityId, Vec3i pos, Color color) {
        EntityDefinition def = m_registry.getEntityDefinition(entityId);
        List<Object> components = new ArrayList<>();

        // Set GameContext so ComponentFactory can create special components
        m_registry.getComponentFactory().setGameContext(context);

        if (color != null) {
            components.add(new ColorComp(color));
        }

        components.addAll(def.instantiateComponents(m_registry));
        components.add(new Position(pos));

        Entity entity = context.ecs().createEntity(components.toArray());
        context.pos().add(entity, pos);

        return entity;
    }

    public Entity createEntityWithOverrides(
        GameContext context,
        String entityId,
        List<ComponentDefinition> overrides
    ) {
        // Create a virtual definition with the base entity as parent
        // and overrides as components - reuses resolveComponents merge logic
        EntityDefinition overrideDef =
            new EntityDefinition(List.of(entityId), overrides);

        // Set GameContext so ComponentFactory can create special components
        m_registry.getComponentFactory().setGameContext(context);

        List<Object> components =
            new ArrayList<>(overrideDef.instantiateComponents(m_registry));

        Entity entity = context.ecs().createEntity(components.toArray());

        // If a Position component was included, register it in PositionContext
        Position pos = entity.get(Position.class);
        if (pos != null) {
            context.pos().add(entity, pos.getPosition());
        }

        return entity;
    }

    public GlyphFactory getGlyphFactory() {
        return m_glyphFactory;
    }

    public void dispose() {
        m_glyphFactory.dispose();
    }
}
