package ca.kieve.ssss.blueprint;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.component.Examinable;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.RenderingHint;
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
            new Descriptor("Wall", "Sturdy."),
            new RenderingHint(1),
            new Examinable(),
            MaterialBlueprint.createStoneComponents()
        );
    }

    public static Entity createFloor(GameContext context, Vec3i pos) {
        return createTile(
            context,
            pos,
            GlyphRepo.INTERPUNCT,
            new Position(pos),
            new Descriptor("Floor", "For walking."),
            new RenderingHint(0),
            MaterialBlueprint.createWoodComponents()
        );
    }

    public static Entity createSteelWall(GameContext context, Vec3i pos) {
        return createTile(
            context,
            pos,
            GlyphRepo.SOLID,
            new Position(pos),
            new Descriptor("Wall", "It's even shiny."),
            new RenderingHint(1),
            new Examinable(),
            MaterialBlueprint.createSteelComponents()
        );
    }

    private static Entity createTile(GameContext context, Vec3i pos, Object ...components) {
        var entity = context.ecs().createEntity(flatten(components));
        context.pos().add(entity, pos);
        return entity;
    }
}
