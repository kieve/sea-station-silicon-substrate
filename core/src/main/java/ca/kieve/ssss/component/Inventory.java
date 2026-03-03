package ca.kieve.ssss.component;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.annotations.EditorIgnore;

import java.util.ArrayList;
import java.util.List;

public record Inventory(
    @EditorIgnore List<Entity> items
) implements Component {
    public Inventory() {
        this(new ArrayList<>());
    }
}
