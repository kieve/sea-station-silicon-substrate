package ca.kieve.ssss.component;


import dev.dominion.ecs.api.Entity;

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
        if (Objects.equals(m_position, position)) return;
        context.pos().move(entity, m_position, position);
        m_position = position;
    }

    @Override
    public void cleanup(GameContext context, Entity entity) {
        context.pos().remove(entity, m_position);
    }

    @Override
    public String toString() {
        return "Position{" +
            "pos=" + m_position +
            '}';
    }
}
