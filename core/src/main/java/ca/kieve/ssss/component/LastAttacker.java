package ca.kieve.ssss.component;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.annotations.EditorIgnore;

/**
 * Component that tracks the last entity to attack this entity.
 * Used for fight-back AI behaviors.
 */
public class LastAttacker implements Component {
    @EditorIgnore
    public Entity attacker;

    public LastAttacker(Entity attacker) {
        this.attacker = attacker;
    }
}
