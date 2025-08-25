package ca.kieve.ssss.component;

import dev.dominion.ecs.api.Entity;

import java.util.ArrayList;
import java.util.List;

public record Inventory(
    List<Entity> items
) implements Component {
    public Inventory() {
        this(new ArrayList<>());
    }
}
