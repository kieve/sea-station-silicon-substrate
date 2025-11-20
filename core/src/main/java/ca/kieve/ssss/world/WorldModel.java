package ca.kieve.ssss.world;

import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.util.Vec3i;

/**
 * Represents a 3D voxel world as a grid of blocks.
 * Each position in the world contains a block type ID string.
 * Uses x-then-y-then-z indexing: blocks[x][y][z]
 */
public class WorldModel {

    private static final String AIR = "air";

    private final int m_width;
    private final int m_height;
    private final int m_depth;
    private final String[][][] m_blocks;
    private final BlockTypeFactory m_blockTypeFactory;

    /**
     * Creates a new world filled with air.
     *
     * @param width  The size in the X direction
     * @param height The size in the Y direction
     * @param depth  The size in the Z direction (number of vertical levels)
     * @param blockTypeFactory Factory for querying block type properties
     */
    public WorldModel(int width, int height, int depth, BlockTypeFactory blockTypeFactory) {
        m_width = width;
        m_height = height;
        m_depth = depth;
        m_blockTypeFactory = blockTypeFactory;
        m_blocks = new String[width][height][depth];

        // Initialize all blocks to air
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    m_blocks[x][y][z] = AIR;
                }
            }
        }
    }

    /**
     * Gets the block type ID at the specified position.
     * Returns "air" if the position is out of bounds.
     */
    public String getBlock(Vec3i pos) {
        return getBlock(pos.x, pos.y, pos.z);
    }

    /**
     * Gets the block type ID at the specified coordinates.
     * Returns "air" if the position is out of bounds.
     */
    public String getBlock(int x, int y, int z) {
        if (!isInBounds(x, y, z)) {
            return AIR;
        }
        return m_blocks[x][y][z];
    }

    /**
     * Sets the block at the specified position.
     * Does nothing if the position is out of bounds.
     */
    public void setBlock(Vec3i pos, String blockTypeId) {
        setBlock(pos.x, pos.y, pos.z, blockTypeId);
    }

    /**
     * Sets the block at the specified coordinates.
     * Does nothing if the position is out of bounds.
     */
    public void setBlock(int x, int y, int z, String blockTypeId) {
        if (!isInBounds(x, y, z)) {
            return;
        }
        m_blocks[x][y][z] = blockTypeId;
    }

    /**
     * Checks if the given position is within world bounds.
     */
    public boolean isInBounds(Vec3i pos) {
        return isInBounds(pos.x, pos.y, pos.z);
    }

    /**
     * Checks if the given coordinates are within world bounds.
     */
    public boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < m_width
                && y >= 0 && y < m_height
                && z >= 0 && z < m_depth;
    }

    /**
     * Checks if an entity can occupy the given position.
     * Returns true if the block at pos is not solid (e.g., air).
     */
    public boolean isPassable(Vec3i pos) {
        return isPassable(pos.x, pos.y, pos.z);
    }

    /**
     * Checks if an entity can occupy the given position.
     * Returns true if the block at pos is not solid (e.g., air).
     */
    public boolean isPassable(int x, int y, int z) {
        return !isSolid(x, y, z);
    }

    /**
     * Checks if the block at the given position is solid.
     */
    public boolean isSolid(Vec3i pos) {
        return isSolid(pos.x, pos.y, pos.z);
    }

    /**
     * Checks if the block at the given position is solid.
     */
    public boolean isSolid(int x, int y, int z) {
        String blockTypeId = getBlock(x, y, z);
        return m_blockTypeFactory.isSolid(blockTypeId);
    }

    /**
     * Returns true if the block at the given position is air.
     */
    public boolean isAir(int x, int y, int z) {
        return AIR.equals(getBlock(x, y, z));
    }

    /**
     * Checks if there is a solid block below the given position.
     * This is used to determine if an entity has ground to stand on.
     */
    public boolean hasFloor(Vec3i pos) {
        return hasFloor(pos.x, pos.y, pos.z);
    }

    /**
     * Checks if there is a solid block below the given position.
     * This is used to determine if an entity has ground to stand on.
     */
    public boolean hasFloor(int x, int y, int z) {
        return isSolid(x, y, z - 1);
    }

    /**
     * Checks if there is a solid block above the given position.
     * This can be used to check for ceilings.
     */
    public boolean hasCeiling(Vec3i pos) {
        return hasCeiling(pos.x, pos.y, pos.z);
    }

    /**
     * Checks if there is a solid block above the given position.
     */
    public boolean hasCeiling(int x, int y, int z) {
        return isSolid(x, y, z + 1);
    }

    /**
     * Checks if a block has at least one adjacent air block.
     * Useful for determining if a block is "visible" and should be rendered.
     */
    public boolean isExposed(int x, int y, int z) {
        // Check all 6 adjacent positions
        return !isSolid(x - 1, y, z)
                || !isSolid(x + 1, y, z)
                || !isSolid(x, y - 1, z)
                || !isSolid(x, y + 1, z)
                || !isSolid(x, y, z - 1)
                || !isSolid(x, y, z + 1);
    }

    public int getWidth() {
        return m_width;
    }

    public int getHeight() {
        return m_height;
    }

    public int getDepth() {
        return m_depth;
    }
}
