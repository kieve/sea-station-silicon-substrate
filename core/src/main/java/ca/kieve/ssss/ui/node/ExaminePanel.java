package ca.kieve.ssss.ui.node;

import ca.kieve.ssss.context.ExamineContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.system.ExamineSystem;
import ca.kieve.ssss.ui.core.UiRenderContext;

import java.util.ArrayList;
import java.util.List;

/**
 * UI panel that displays names of entities under the examine crosshair.
 * Visible tiles show live entities; explored-but-not-visible tiles show
 * remembered (ghost) entities; unexplored tiles show nothing.
 */
public class ExaminePanel extends SelectionPanel {
    private List<String> m_entityNames = new ArrayList<>();

    @Override
    public void update(UiRenderContext renderContext, float delta) {
        var gc = renderContext.gameContext();
        var examineContext = gc.examine();

        boolean active = examineContext.isActive();
        setActive(active);
        if (!active) {
            return;
        }

        setShowSelectionIndicator(examineContext.isSelectionMode());
        setSelectedIndex(examineContext.getSelectedIndex());

        var crosshair = examineContext.getCrosshairPos();
        var vision = gc.vision();
        boolean visible = gc.debug().isFullVision() || vision.isVisible(crosshair.x, crosshair.y);
        boolean explored = vision.isExplored(crosshair.x, crosshair.y, crosshair.z);

        m_entityNames.clear();
        if (visible) {
            populateFromCurrent(gc, examineContext);
        } else if (explored) {
            populateFromGhost(gc, examineContext);
        }
        // unexplored: leave the list empty so the panel hides itself
        setItems(m_entityNames);
    }

    private void populateFromCurrent(GameContext gc, ExamineContext examineContext) {
        var items = ExamineSystem.visibleItems(gc.pos(), gc.fluid(), examineContext);
        for (var item : items) {
            String displayName = switch (item.type()) {
            case MAIN -> item.name();
            case FLOOR -> item.name() + " (Floor)";
            case CEILING -> item.name() + " (Ceiling)";
            };
            m_entityNames.add(displayName);
        }
    }

    private void populateFromGhost(GameContext gc, ExamineContext examineContext) {
        var crosshair = examineContext.getCrosshairPos();
        var ghost = gc.vision().getGhost(crosshair.x, crosshair.y, crosshair.z);
        if (ghost == null) {
            return;
        }
        for (var ghostEntity : ghost.entities) {
            String displayName = switch (ghostEntity.type()) {
            case MAIN -> ghostEntity.name() + " (memory)";
            case FLOOR -> ghostEntity.name() + " (memory, floor)";
            case CEILING -> ghostEntity.name() + " (memory, ceiling)";
            };
            m_entityNames.add(displayName);
        }
    }

    public List<String> getEntityNames() {
        return m_entityNames;
    }
}
