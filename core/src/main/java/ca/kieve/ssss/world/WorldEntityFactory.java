package ca.kieve.ssss.world;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.util.Vec3i;

/**
 * Converts a WorldModel into ECS entities.
 * Only creates entities for solid blocks that are adjacent to air (exposed blocks).
 * This optimization prevents creating entities for blocks that will never be visible.
 */
public class WorldEntityFactory {

    private WorldEntityFactory() {
        // Do not instantiate
    }

    /**
     * Creates ECS entities for all exposed blocks in the world model.
     * An exposed block is a solid block that has at least one adjacent air block.
     *
     * @param context The game context
     * @param world   The world model to convert
     * @return The number of block entities created
     */
    public static int createEntities(GameContext context, WorldModel world) {
        int count = 0;
        var factory = context.entityFactory();

        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                for (int z = 0; z < world.getDepth(); z++) {
                    String blockTypeId = world.getBlock(x, y, z);

                    // Skip air blocks - they don't have entities
                    if (world.isAir(x, y, z)) {
                        continue;
                    }

                    // Only create entities for exposed blocks (adjacent to air)
                    if (!world.isExposed(x, y, z)) {
                        continue;
                    }

                    var pos = new Vec3i(x, y, z);
                    var entity = factory.createEntity(context, blockTypeId, pos);

                    if (entity != null) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * Creates ECS entities for all solid blocks in the world model,
     * regardless of whether they are exposed.
     * Use this for debugging or small maps where optimization isn't needed.
     *
     * @param context The game context
     * @param world   The world model to convert
     * @return The number of block entities created
     */
    public static int createAllEntities(GameContext context, WorldModel world) {
        int count = 0;
        var factory = context.entityFactory();

        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                for (int z = 0; z < world.getDepth(); z++) {
                    String blockTypeId = world.getBlock(x, y, z);

                    // Skip air blocks
                    if (world.isAir(x, y, z)) {
                        continue;
                    }

                    var pos = new Vec3i(x, y, z);
                    var entity = factory.createEntity(context, blockTypeId, pos);

                    if (entity != null) {
                        count++;
                    }
                }
            }
        }

        return count;
    }
}
