package ca.kieve.ssss.blueprint;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.repository.GlyphRepo;
import ca.kieve.ssss.util.Vec3i;

import static ca.kieve.ssss.util.ListUtil.flatten;

public class TileBlueprints {
    private TileBlueprints() {
        // Do not instantiate
    }

    public static Entity createWall(GameContext context, Vec3i pos) {
        return createTile(
            context,
            pos,
            GlyphRepo.POUND,
            new Position(pos),
            new Descriptor("Stone wall", "It's a stone wall."),
            MaterialBlueprint.createStoneComponents()
        );
    }

    public static Entity createFloor(GameContext context, Vec3i pos) {
        return createTile(
            context,
            pos,
            GlyphRepo.INTERPUNCT,
            new Position(pos),
            MaterialBlueprint.createWoodComponents()
        );
    }

    public static Entity createSteelWall(GameContext context, Vec3i pos) {
        return createTile(
            context,
            pos,
            GlyphRepo.SOLID,
            new Position(pos),
            new Descriptor("Steel wall", "It's a steel wall."),
            MaterialBlueprint.createSteelComponents()
        );
    }

    private static Entity createTile(GameContext context, Vec3i pos, Object ...components) {
        var entity = context.ecs().createEntity(flatten(components));
        context.pos().add(entity, pos);
        return entity;
    }
}
