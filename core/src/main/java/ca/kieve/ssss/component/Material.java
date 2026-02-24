package ca.kieve.ssss.component;

import ca.kieve.ssss.annotations.EditorRef;
import dev.dominion.ecs.api.Entity;

public record Material(
    @EditorRef(
            value = "id",
            source = EditorRef.Source.ENTITY)
    Entity entity
) implements Component {
}
