package ca.kieve.ssss.blueprint;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Density;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.repository.GlyphRepo;
import ca.kieve.ssss.util.Vec3i;

public class TileBlueprints {
    private TileBlueprints() {
        // Do not instantiate
    }

    public static Entity createWall(GameContext context, Vec3i pos) {
        var entity = context.ecs().createEntity(
            GlyphRepo.POUND,
            new Position(pos),
            Density.SOLID
        );
        context.pos().add(entity, pos);
        return entity;
    }

    public static Entity createFloor(GameContext context, Vec3i pos) {
        var entity = context.ecs().createEntity(
            GlyphRepo.INTERPUNCT,
            new Position(pos)
        );
        context.pos().add(entity, pos);
        return entity;
    }
}
