package ca.kieve.ssss.event;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;

public record Interaction(Verb verb, Entity entity) {
    public enum Verb {
        PICK_UP("Pick up"),
        OPEN("Open"),
        CLOSE("Close"),
        UNLOCK("Unlock"),
        LOCK("Lock");

        private final String m_label;

        Verb(String label) {
            m_label = label;
        }

        public String label() {
            return m_label;
        }
    }

    public String getLabel() {
        var descriptor = entity.get(Descriptor.class);
        String name = descriptor != null ? descriptor.name() : "???";
        return verb.label() + " " + name;
    }
}
