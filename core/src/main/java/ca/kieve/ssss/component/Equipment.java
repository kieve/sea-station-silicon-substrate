package ca.kieve.ssss.component;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.annotations.EditorRef;

public class Equipment implements Component {
    @EditorRef(value = "weaponId", source = EditorRef.Source.ENTITY)
    public Entity weapon;

    public Equipment() {
        this(null);
    }

    public Equipment(Entity weapon) {
        this.weapon = weapon;
    }
}
