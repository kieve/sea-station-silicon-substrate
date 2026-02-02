package ca.kieve.ssss.context;

import com.badlogic.gdx.graphics.Color;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.RenderingHint;
import ca.kieve.ssss.ui.TileHighlight;
import ca.kieve.ssss.ui.TileHighlightProvider;
import ca.kieve.ssss.util.Vec3i;
import dev.dominion.ecs.api.Entity;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ExamineContext implements TileHighlightProvider {
    private RenderContext m_renderContext;
    private boolean m_active = false;
    private Vec3i m_crosshairPos = new Vec3i(0, 0, 0);
    private boolean m_selectionMode = false;
    private int m_selectedIndex = 0;

    /**
     * Sorts entities by Z-index in descending order (highest Z-index first).
     * Filters to only include entities with a Descriptor component.
     */
    public static List<Entity> sortEntitiesByZIndex(List<Entity> entities) {
        return entities.stream()
            .filter(entity -> entity.get(Descriptor.class) != null)
            .sorted(Comparator.comparingInt(entity -> {
                var hint = entity.get(RenderingHint.class);
                return hint != null ? -hint.zIndex : 0;
            }))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isHighlightActive() {
        return m_active;
    }

    @Override
    public List<TileHighlight> getHighlights() {
        return List.of(new TileHighlight(
            m_crosshairPos.x, m_crosshairPos.y, Color.YELLOW));
    }

    public void init(GameContext gameContext) {
        m_renderContext = gameContext.render();
    }

    public boolean isActive() {
        return m_active;
    }

    public void enter(Vec3i startPos) {
        m_active = true;
        m_crosshairPos.set(startPos);
        m_selectionMode = false;
        m_selectedIndex = 0;
        markDirty();
    }

    public void exit() {
        m_active = false;
        m_selectionMode = false;
        m_selectedIndex = 0;
        markDirty();
    }

    public Vec3i getCrosshairPos() {
        return m_crosshairPos;
    }

    /**
     * Gets the position of the floor (block below the crosshair).
     */
    public Vec3i getFloorPos() {
        return new Vec3i(m_crosshairPos.x, m_crosshairPos.y, m_crosshairPos.z - 1);
    }

    /**
     * Gets the position of the ceiling (block above the crosshair).
     */
    public Vec3i getCeilingPos() {
        return new Vec3i(m_crosshairPos.x, m_crosshairPos.y, m_crosshairPos.z + 1);
    }

    public void moveCrosshair(Vec3i delta) {
        m_crosshairPos.addMut(delta);
        markDirty();
    }

    private void markDirty() {
        if (m_renderContext != null) {
            m_renderContext.markDirty();
        }
    }

    public boolean isSelectionMode() {
        return m_selectionMode;
    }

    public void enterSelectionMode() {
        m_selectionMode = true;
        m_selectedIndex = 0;
    }

    public void exitSelectionMode() {
        m_selectionMode = false;
        m_selectedIndex = 0;
    }

    public int getSelectedIndex() {
        return m_selectedIndex;
    }

    public void setSelectedIndex(int index) {
        m_selectedIndex = index;
    }

    public void incrementSelectedIndex(int maxIndex) {
        m_selectedIndex = (m_selectedIndex + 1) % maxIndex;
    }

    public void decrementSelectedIndex(int maxIndex) {
        m_selectedIndex = (m_selectedIndex - 1 + maxIndex) % maxIndex;
    }
}
