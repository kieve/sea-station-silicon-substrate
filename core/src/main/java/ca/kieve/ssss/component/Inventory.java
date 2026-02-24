package ca.kieve.ssss.component;

import java.util.ArrayList;
import java.util.List;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.annotations.EditorIgnore;

public record Inventory(
    @EditorIgnore List<Entity> items
) implements Component {
    public Inventory() {
        this(new ArrayList<>());
    }
}
