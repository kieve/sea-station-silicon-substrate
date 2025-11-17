package ca.kieve.ssss.world;

import ca.kieve.ssss.util.Vec3i;

/**
 * A simple static map generator for testing purposes.
 * Creates two rooms connected by a short hallway.
 *
 * Layout (top-down view at Z=1, where entities exist):
 *
 * Room 1 (5x5 interior):     Room 2 (5x5 interior):
 * #######                    #######
 * #.....#                    #.....#
 * #.....#  ##                #.....#
 * #.....####..####           #.....#
 * #.....#        #############.....#
 * #.....####..####           #.....#
 * #.....#  ##                #.....#
 * #######                    #######
 *
 * Where:
 * # = Stone wall (solid block from Z=0 to Z=2)
 * . = Open space (wood floor at Z=0, air at Z=1)
 */
public class StaticTestMapGenerator implements MapGenerator {

    private static final int WORLD_WIDTH = 40;
    private static final int WORLD_HEIGHT = 25;
    private static final int WORLD_DEPTH = 4;

    // Room 1 position (top-left corner of interior)
    private static final int ROOM1_X = 1;
    private static final int ROOM1_Y = 4;
    private static final int ROOM1_WIDTH = 15;
    private static final int ROOM1_HEIGHT = 15;

    // Room 2 position (top-left corner of interior)
    private static final int ROOM2_X = 24;
    private static final int ROOM2_Y = 9;
    private static final int ROOM2_WIDTH = 5;
    private static final int ROOM2_HEIGHT = 5;

    // Hallway connects the rooms
    private static final int HALLWAY_Y = 11;
    private static final int HALLWAY_HEIGHT = 2;

    private Vec3i m_playerSpawn;

    @Override
    public WorldModel generate() {
        var world = new WorldModel(WORLD_WIDTH, WORLD_HEIGHT, WORLD_DEPTH);

        // Create room 1
        createRoom(world, ROOM1_X, ROOM1_Y, ROOM1_WIDTH, ROOM1_HEIGHT);

        // Create room 2
        createRoom(world, ROOM2_X, ROOM2_Y, ROOM2_WIDTH, ROOM2_HEIGHT);

        // Create hallway connecting the rooms
        createHallway(world);

        // Set player spawn to center of room 1
        int spawnX = ROOM1_X + ROOM1_WIDTH / 2;
        int spawnY = ROOM1_Y + ROOM1_HEIGHT / 2;
        m_playerSpawn = new Vec3i(spawnX, spawnY, 1);

        return world;
    }

    @Override
    public Vec3i getPlayerSpawn() {
        return m_playerSpawn;
    }

    private void createRoom(WorldModel world, int x, int y, int width, int height) {
        // Create floor (Z=0) with wood blocks for the entire room including walls
        for (int dx = -1; dx <= width; dx++) {
            for (int dy = -1; dy <= height; dy++) {
                world.setBlock(x + dx, y + dy, 0, BlockData.WOOD);
            }
        }

        // Create walls (Z=0, 1, 2) around the perimeter
        // Top and bottom walls
        for (int dx = -1; dx <= width; dx++) {
            // Top wall (y - 1)
            setWallColumn(world, x + dx, y - 1);
            // Bottom wall (y + height)
            setWallColumn(world, x + dx, y + height);
        }

        // Left and right walls
        for (int dy = 0; dy < height; dy++) {
            // Left wall (x - 1)
            setWallColumn(world, x - 1, y + dy);
            // Right wall (x + width)
            setWallColumn(world, x + width, y + dy);
        }

        // Interior is already air (default), but ensure it's clear at Z=1
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                world.setBlock(x + dx, y + dy, 1, BlockData.AIR);
            }
        }
    }

    private void createHallway(WorldModel world) {
        // Hallway runs from right edge of room 1 to left edge of room 2
        int hallwayStartX = ROOM1_X + ROOM1_WIDTH;
        int hallwayEndX = ROOM2_X - 1;

        // Create hallway floor and clear interior
        for (int hx = hallwayStartX; hx <= hallwayEndX; hx++) {
            for (int dy = 0; dy < HALLWAY_HEIGHT; dy++) {
                int hy = HALLWAY_Y + dy;
                // Floor at Z=0
                world.setBlock(hx, hy, 0, BlockData.WOOD);
                // Clear space at Z=1
                world.setBlock(hx, hy, 1, BlockData.AIR);
            }
        }

        // Create hallway walls (top and bottom of hallway)
        for (int hx = hallwayStartX; hx <= hallwayEndX; hx++) {
            // Top wall of hallway
            setWallColumn(world, hx, HALLWAY_Y - 1);
            // Bottom wall of hallway
            setWallColumn(world, hx, HALLWAY_Y + HALLWAY_HEIGHT);
        }
    }

    private void setWallColumn(WorldModel world, int x, int y) {
        // Walls are stone blocks from Z=0 to Z=2 (3 blocks tall)
        world.setBlock(x, y, 0, BlockData.STONE);
        world.setBlock(x, y, 1, BlockData.STONE);
        world.setBlock(x, y, 2, BlockData.STONE);
    }
}
