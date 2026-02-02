package ca.kieve.ssss.event;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;

public record Interaction(String verb, Entity entity) {
    public String getLabel() {
        var descriptor = entity.get(Descriptor.class);
        String name = descriptor != null ? descriptor.name() : "???";
        return verb + " " + name;
    }
}
