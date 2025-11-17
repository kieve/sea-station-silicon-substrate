package ca.kieve.ssss.context;

import ca.kieve.ssss.component.Density;
import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.util.Vec3i;

import java.util.HashMap;
import java.util.Map;

public class EjectContext {
    private RenderContext m_renderContext;
    private PositionContext m_positionContext;
    private boolean m_active = false;
    private Vec3i m_currentPos = new Vec3i(0, 0, 0);
    private final Map<Vec3i, Boolean> m_validDirections = new HashMap<>();

    public void init(GameContext gameContext) {
        m_renderContext = gameContext.render();
        m_positionContext = gameContext.pos();
    }

    public boolean isActive() {
        return m_active;
    }

    public void enter(Vec3i currentPos) {
        m_active = true;
        m_currentPos.set(currentPos);
        calculateValidDirections();
        markDirty();
    }

    public void exit() {
        m_active = false;
        m_validDirections.clear();
        markDirty();
    }

    private void markDirty() {
        if (m_renderContext != null) {
            m_renderContext.markDirty();
        }
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

    private void calculateValidDirections() {
        m_validDirections.clear();
        if (m_positionContext == null) {
            return;
        }

        Vec3i[] directions = {Vec3i.NORTH, Vec3i.SOUTH, Vec3i.EAST, Vec3i.WEST};
        for (Vec3i dir : directions) {
            Vec3i targetPos = m_currentPos.add(dir);
            var entities = m_positionContext.getAt(targetPos);
            var blocked = entities.stream().anyMatch(entity -> {
                // Block if solid (walls, etc.)
                var density = entity.get(Density.class);
                if (density == Density.SOLID) {
                    return true;
                }
                // Block if another body (dead mech, etc.) occupies the space
                if (entity.has(Socket.class)) {
                    return true;
                }
                return false;
            });
            m_validDirections.put(dir, !blocked);
        }
    }
}
