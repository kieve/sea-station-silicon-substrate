package ca.kieve.ssss.blueprint;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Inventory;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.component.WasdController;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.repository.ComponentRepo;
import ca.kieve.ssss.repository.GlyphRepo;
import ca.kieve.ssss.util.Vec3i;

public class PlayerBlueprint {
    private PlayerBlueprint() {
        // Do not instantiate
    }

    public static Entity create(GameContext context, Vec3i pos) {
        var wasdController = new WasdController();
        var entity = context.ecs().createEntity(
            // Display
            GlyphRepo.PLAYER,
            new Descriptor("The Player", "It's you!"),

            // Control
            new CameraComp(),
            wasdController,
            new Speed(100),

            // Physics
            new Position(pos),
            new Velocity(),
            ComponentRepo.COLLIDER,

            // Extras
            new Inventory()
        );
        context.inputMux().addProcessor(wasdController);
        context.pos().add(entity, pos);
        return entity;
    }
}
