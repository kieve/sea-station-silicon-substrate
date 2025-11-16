package ca.kieve.ssss.component;

import ca.kieve.ssss.event.EventType;

/**
 * Component that marks an entity as interactable when bumped into.
 * The eventType determines what kind of interaction occurs.
 */
public class InteractComponent implements Component {
    public EventType eventType;
    public boolean blocksMovement;

    public InteractComponent(EventType eventType) {
        this(eventType, true);
    }

    public InteractComponent(EventType eventType, boolean blocksMovement) {
        this.eventType = eventType;
        this.blocksMovement = blocksMovement;
    }

    @Override
    public String toString() {
        return "InteractComponent{" +
            "eventType=" + eventType +
            ", blocksMovement=" + blocksMovement +
            '}';
    }
}
