package ca.kieve.ssss.ui.node;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.context.ExamineContext;
import ca.kieve.ssss.system.ExamineSystem.ExamineItem;
import ca.kieve.ssss.ui.core.UiRenderContext;

import java.util.ArrayList;
import java.util.List;

/**
 * UI panel that displays names of entities under the examine crosshair.
 * Shows '>' prefix for selected entity in selection mode.
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

        // Build the list of examine items (main level + ceiling + floor)
        var items = new ArrayList<ExamineItem>();

        // Get entities at main level (crosshair position)
        var mainPos = examineContext.getCrosshairPos();
        var mainEntities = ExamineContext.sortEntitiesByZIndex(gc.pos().getAt(mainPos));
        for (var entity : mainEntities) {
            items.add(new ExamineItem(entity, ExamineItem.ItemType.MAIN));
        }

        // Get entities at ceiling level (z+1)
        var ceilingPos = examineContext.getCeilingPos();
        var ceilingEntities = ExamineContext.sortEntitiesByZIndex(gc.pos().getAt(ceilingPos));
        for (var entity : ceilingEntities) {
            items.add(new ExamineItem(entity, ExamineItem.ItemType.CEILING));
        }

        // Get entities at floor level (z-1)
        var floorPos = examineContext.getFloorPos();
        var floorEntities = ExamineContext.sortEntitiesByZIndex(gc.pos().getAt(floorPos));
        for (var entity : floorEntities) {
            items.add(new ExamineItem(entity, ExamineItem.ItemType.FLOOR));
        }

        // Convert items to display names
        m_entityNames.clear();
        for (var item : items) {
            var name = item.entity().get(Descriptor.class).name();
            String displayName = switch (item.type()) {
            case MAIN -> name;
            case FLOOR -> name + " (Floor)";
            case CEILING -> name + " (Ceiling)";
            };
            m_entityNames.add(displayName);
        }

        setItems(m_entityNames);
    }

    public List<String> getEntityNames() {
        return m_entityNames;
    }
}
