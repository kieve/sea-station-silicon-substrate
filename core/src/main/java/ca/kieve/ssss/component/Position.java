package ca.kieve.ssss.component;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.content.ComponentDefinition;
import ca.kieve.ssss.content.MapEntityDefinition;
import ca.kieve.ssss.content.PropertyParser;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;

import java.util.Objects;

public class Position implements Component {
    private Vec3i m_position;

    public Position() {
        this(0, 0, 0);
    }

    public Position(int x, int y, int z) {
        this(new Vec3i(x, y, z));
    }

    public Position(Vec3i position) {
        m_position = position;
    }

    public Vec3i getPosition() {
        return m_position;
    }

    public void setPosition(GameContext context, Entity entity, Vec3i position) {
        if (Objects.equals(m_position, position)) {
            return;
        }
        context.pos().move(entity, m_position, position);
        m_position = position;
    }

    @Override
    public void cleanup(GameContext context, Entity entity) {
        context.pos().remove(entity, m_position);
    }

    @Override
    public String toString() {
        return "Position{"
            + "pos=" + m_position
            + '}';
    }

    /**
     * Extracts a {@link Vec3i} from a Position component on the given
     * map entity definition. Returns {@code null} when the entity has
     * no Position component. Missing x/y/z properties default to 0.
     *
     * <p>This is the canonical Position reader for load-time consumers
     * (composite map loader, editor flattener) — anywhere that needs to
     * pull a position out of a {@link MapEntityDefinition} without
     * instantiating a runtime {@code Position} object.
     */
    public static Vec3i readFromDefinition(MapEntityDefinition entity) {
        for (ComponentDefinition comp : entity.components()) {
            if (comp.type() != Position.class) {
                continue;
            }
            return readFromDefinition(comp);
        }
        return null;
    }

    /**
     * Extracts a {@link Vec3i} from a Position {@link ComponentDefinition}.
     * Returns {@code null} if the definition is not a Position. Missing
     * x/y/z properties default to 0.
     */
    public static Vec3i readFromDefinition(ComponentDefinition comp) {
        if (comp.type() != Position.class) {
            return null;
        }
        return new Vec3i(
            intValue(comp.properties().get("x")),
            intValue(comp.properties().get("y")),
            intValue(comp.properties().get("z"))
        );
    }

    private static int intValue(Object value) {
        if (value == null) {
            return 0;
        }
        return (Integer) PropertyParser.parseValue(value, Integer.class);
    }
}
