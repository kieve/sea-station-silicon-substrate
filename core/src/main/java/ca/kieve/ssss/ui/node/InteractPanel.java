package ca.kieve.ssss.ui.node;

import java.util.ArrayList;
import java.util.List;

import ca.kieve.ssss.context.InteractContext.Phase;
import ca.kieve.ssss.event.Interaction;
import ca.kieve.ssss.ui.core.UiRenderContext;

/**
 * UI panel that displays available interactions when multiple options exist.
 * Shows '>' prefix for the currently selected interaction.
 */
public class InteractPanel extends SelectionPanel {
    @Override
    public void update(UiRenderContext renderContext, float delta) {
        var gc = renderContext.gameContext();
        var interactContext = gc.interact();

        boolean active = interactContext.isActive()
            && interactContext.getPhase() == Phase.ITEM_SELECT;
        setActive(active);
        if (!active) {
            return;
        }

        setSelectedIndex(interactContext.getSelectedIndex());
        setShowSelectionIndicator(true);

        List<Interaction> interactions = interactContext.getInteractions();
        var labels = new ArrayList<String>();
        for (var interaction : interactions) {
            labels.add(interaction.getLabel());
        }
        setItems(labels);
    }
}
