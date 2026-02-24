package ca.kieve.ssss.component;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.annotations.EditorRef;

public record Material(
    @EditorRef(
            value = "id",
            source = EditorRef.Source.ENTITY)
    Entity entity
) implements Component {
}
