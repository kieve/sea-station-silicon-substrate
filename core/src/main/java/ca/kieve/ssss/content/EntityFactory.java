package ca.kieve.ssss.content;

import com.badlogic.gdx.graphics.Color;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.ColorComp;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Speed;
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

    public Entity createBlock(GameContext context, Vec3i pos, String blockTypeId) {
        if ("air".equals(blockTypeId)) {
            return createEntity(context, "air", pos);
        }
        String entityId = "block_" + blockTypeId;
        return createEntity(context, entityId, pos);
    }

    public Entity createDebugMover(
        GameContext context,
        Vec3i pos,
        int speed,
        Color color
    ) {
        Entity entity = createEntity(context, "debugMover", pos, color);
        entity.add(new Speed(speed));
        return entity;
    }

    public GlyphFactory getGlyphFactory() {
        return m_glyphFactory;
    }

    public void dispose() {
        m_glyphFactory.dispose();
    }
}
