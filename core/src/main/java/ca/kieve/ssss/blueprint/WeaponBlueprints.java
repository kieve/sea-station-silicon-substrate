package ca.kieve.ssss.blueprint;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Damage;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.context.GameContext;

public class WeaponBlueprints {
    private WeaponBlueprints() {
        // Do not instantiate
    }

    public static Entity createChipClaws(GameContext context) {
        return context.ecs().createEntity(
            new Descriptor("Chip Claws", "They almost hurt"),
            new Damage(1)
        );
    }

    public static Entity createPowerFist(GameContext context) {
        return context.ecs().createEntity(
            new Descriptor("Power Fist", "POW"),
            new Damage(10)
        );
    }
}
