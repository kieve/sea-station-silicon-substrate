package ca.kieve.ssss.context;

import com.badlogic.gdx.graphics.Color;

import ca.kieve.ssss.component.Item;
import ca.kieve.ssss.event.Interaction;
import ca.kieve.ssss.ui.TileHighlight;
import ca.kieve.ssss.ui.TileHighlightProvider;
import ca.kieve.ssss.util.Vec3i;
import dev.dominion.ecs.api.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InteractContext implements TileHighlightProvider {
    private static final Color VALID_COLOR = Color.GREEN;
    public enum Phase {
        DIRECTION_SELECT,
        ITEM_SELECT
    }

    private RenderContext m_renderContext;
    private PositionContext m_positionContext;
    private boolean m_active = false;
    private Phase m_phase = Phase.DIRECTION_SELECT;
    private Vec3i m_currentPos;
    private final Map<Vec3i, Boolean> m_validDirections = new HashMap<>();
    private Vec3i m_targetPos;
    private List<Interaction> m_interactions = List.of();
    private int m_selectedIndex = 0;

    @Override
    public boolean isHighlightActive() {
        return m_active && m_phase == Phase.DIRECTION_SELECT;
    }

    @Override
    public List<TileHighlight> getHighlights() {
        var highlights = new ArrayList<TileHighlight>();
        for (var entry : m_validDirections.entrySet()) {
            if (!entry.getValue()) {
                continue;
            }
            Vec3i direction = entry.getKey();
            Vec3i targetPos = m_currentPos.add(direction);
            highlights.add(new TileHighlight(
                targetPos.x, targetPos.y, VALID_COLOR));
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

    public Phase getPhase() {
        return m_phase;
    }

    public Vec3i getCurrentPos() {
        return m_currentPos;
    }

    public Map<Vec3i, Boolean> getValidDirections() {
        return m_validDirections;
    }

    public void enter(Vec3i playerPos) {
        m_active = true;
        m_phase = Phase.DIRECTION_SELECT;
        m_currentPos = playerPos;
        m_targetPos = null;
        m_interactions = List.of();
        m_selectedIndex = 0;
        calculateValidDirections();
        markDirty();
    }

    public void exit() {
        m_active = false;
        m_phase = Phase.DIRECTION_SELECT;
        m_currentPos = null;
        m_validDirections.clear();
        m_targetPos = null;
        m_interactions = List.of();
        m_selectedIndex = 0;
        markDirty();
    }

    public void selectTarget(Vec3i pos) {
        m_targetPos = pos;
        m_interactions = resolveInteractions(pos);
        m_selectedIndex = 0;
    }

    public void enterItemSelect() {
        m_phase = Phase.ITEM_SELECT;
        markDirty();
    }

    public List<Interaction> getInteractions() {
        return m_interactions;
    }

    public int getSelectedIndex() {
        return m_selectedIndex;
    }

    public void incrementSelectedIndex() {
        if (m_interactions.isEmpty()) {
            return;
        }
        m_selectedIndex = (m_selectedIndex + 1) % m_interactions.size();
    }

    public void decrementSelectedIndex() {
        if (m_interactions.isEmpty()) {
            return;
        }
        m_selectedIndex =
            (m_selectedIndex - 1 + m_interactions.size()) % m_interactions.size();
    }

    private List<Interaction> resolveInteractions(Vec3i pos) {
        var entities = m_positionContext.getAt(pos);
        var result = new ArrayList<Interaction>();
        for (Entity entity : entities) {
            if (entity.has(Item.class)) {
                result.add(new Interaction("Pick up", entity));
            }
        }
        return result;
    }

    public boolean selfHasInteractions() {
        return m_validDirections.getOrDefault(Vec3i.ZERO, false);
    }

    private void calculateValidDirections() {
        m_validDirections.clear();
        if (m_positionContext == null || m_currentPos == null) {
            return;
        }

        Vec3i[] directions = {
            Vec3i.NORTH, Vec3i.SOUTH, Vec3i.EAST, Vec3i.WEST, Vec3i.ZERO
        };
        for (Vec3i dir : directions) {
            Vec3i targetPos = m_currentPos.add(dir);
            boolean hasInteractable = hasInteractableEntities(targetPos);
            m_validDirections.put(dir, hasInteractable);
        }
    }

    private boolean hasInteractableEntities(Vec3i pos) {
        var entities = m_positionContext.getAt(pos);
        for (Entity entity : entities) {
            if (entity.has(Item.class)) {
                return true;
            }
        }
        return false;
    }

    private void markDirty() {
        if (m_renderContext != null) {
            m_renderContext.markDirty();
        }
    }
}
