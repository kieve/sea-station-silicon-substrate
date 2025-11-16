package ca.kieve.ssss.context;

import ca.kieve.ssss.component.Density;
import ca.kieve.ssss.util.Vec3i;

import java.util.HashMap;
import java.util.Map;

public class EjectContext {
    private boolean m_active = false;
    private Vec3i m_currentPos = new Vec3i(0, 0, 0);
    private final Map<Vec3i, Boolean> m_validDirections = new HashMap<>();

    public boolean isActive() {
        return m_active;
    }

    public void enter(Vec3i currentPos, GameContext gameContext) {
        m_active = true;
        m_currentPos.set(currentPos);
        calculateValidDirections(gameContext);
    }

    public void exit() {
        m_active = false;
        m_validDirections.clear();
    }

    public Vec3i getCurrentPos() {
        return m_currentPos;
    }

    public boolean isDirectionValid(Vec3i direction) {
        return m_validDirections.getOrDefault(direction, false);
    }

    public Map<Vec3i, Boolean> getValidDirections() {
        return m_validDirections;
    }

    private void calculateValidDirections(GameContext gameContext) {
        m_validDirections.clear();

        Vec3i[] directions = {Vec3i.NORTH, Vec3i.SOUTH, Vec3i.EAST, Vec3i.WEST};
        for (Vec3i dir : directions) {
            Vec3i targetPos = m_currentPos.add(dir);
            var entities = gameContext.pos().getAt(targetPos);
            var solid = entities.stream().anyMatch(entity -> {
                var density = entity.get(Density.class);
                return density == Density.SOLID;
            });
            m_validDirections.put(dir, !solid);
        }
    }
}
