package ca.kieve.ssss.blueprint;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.CameraComp;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.WasdController;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.repository.GlyphRepo;
import ca.kieve.ssss.util.Vec3i;

public class PlayerBlueprint {
    private PlayerBlueprint() {
        // Do not instantiate
    }

    public static Entity create(GameContext context, Vec3i pos) {
        var wasdController = new WasdController();
        var entity = context.ecs().createEntity(
            GlyphRepo.PLAYER,
            new CameraComp(),
            wasdController,
            new Position(pos)
        );
        context.inputMux().addProcessor(wasdController);
        context.pos().add(entity, pos);
        return entity;
    }
}
