package ca.kieve.ssss.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;

import ca.kieve.ssss.component.Socket;
import ca.kieve.ssss.component.Solid;
import ca.kieve.ssss.ui.TileHighlight;
import ca.kieve.ssss.ui.TileHighlightProvider;
import ca.kieve.ssss.util.Vec3i;

public class EjectContext implements TileHighlightProvider {
    private static final Color VALID_COLOR = Color.GREEN;
    private static final Color INVALID_COLOR = Color.RED;
    private RenderContext m_renderContext;
    private PositionContext m_positionContext;
    private boolean m_active = false;
    private Vec3i m_currentPos = new Vec3i(0, 0, 0);
    private final Map<Vec3i, Boolean> m_validDirections = new HashMap<>();

    @Override
    public boolean isHighlightActive() {
        return m_active;
    }

    @Override
    public List<TileHighlight> getHighlights() {
        var highlights = new ArrayList<TileHighlight>();
        for (var entry : m_validDirections.entrySet()) {
            Vec3i direction = entry.getKey();
            boolean isValid = entry.getValue();
            Vec3i targetPos = m_currentPos.add(direction);
            highlights.add(new TileHighlight(
                targetPos.x, targetPos.y,
                isValid ? VALID_COLOR : INVALID_COLOR));
        }
        return highlights;
    }

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
                if (entity.has(Solid.class)) {
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
