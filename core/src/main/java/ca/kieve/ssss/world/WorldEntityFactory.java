package ca.kieve.ssss.world;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.LambdaCount;

/**
 * Converts a {@link WorldModel} into ECS entities.
 *
 * <p>One entity per non-air cell, no exposure optimisation: future mining
 * / excavation will turn previously-buried blocks into the player's
 * neighbourhood, and a "create on reveal" path is fragile compared to
 * just keeping the entities around from the start. Memory cost scales
 * with total solid cells, which is small at current map sizes; revisit
 * if maps grow past 100k cells.
 */
public class WorldEntityFactory {
    private WorldEntityFactory() {
        // Do not instantiate
    }

    /**
     * Creates an ECS entity for every non-air cell in
     * {@code context.world()}.
     *
     * @return the number of block entities created
     */
    public static int createEntities(GameContext context) {
        WorldModel world = context.world().getModel();
        var factory = context.entityFactory();
        LambdaCount count = new LambdaCount();

        world.box().forEach(cell -> {
            if (world.isAir(cell)) {
                return;
            }
            var entity = factory.createEntity(context, world.getBlock(cell), cell);
            if (entity != null) {
                count.increment();
            }
        });
        return count.get();
    }
}
