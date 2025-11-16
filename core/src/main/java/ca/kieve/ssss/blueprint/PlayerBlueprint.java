package ca.kieve.ssss.blueprint;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.component.DebugRect;
import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Inventory;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.SocketPlug;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.Velocity;
import ca.kieve.ssss.component.WasdController;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.repository.GlyphRepo;
import ca.kieve.ssss.util.Vec3i;

import static ca.kieve.ssss.repository.ComponentRepo.COLLIDER;

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
            new DebugRect(),

            // Control
            new CameraComp(),
            wasdController,
            new Speed(100),
            new SocketPlug(),

            // Physics
            new Position(pos),
            new Velocity(),
            COLLIDER,

            // Extras
            new Inventory()
        );
        context.inputMux().addProcessor(wasdController);
        context.pos().add(entity, pos);
        return entity;
    }
}
