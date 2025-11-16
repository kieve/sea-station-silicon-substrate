package ca.kieve.ssss.component;

import dev.dominion.ecs.api.Entity;

public class Equipment implements Component {
    public Entity weapon;

    public Equipment() {
        this(null);
    }

    public Equipment(Entity weapon) {
        this.weapon = weapon;
    }
}
