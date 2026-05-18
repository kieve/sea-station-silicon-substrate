package ca.kieve.ssss.world;

import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.util.BoundingBox3i;
import ca.kieve.ssss.util.Vec3i;

/**
 * Represents a 3D voxel world as a grid of blocks.
 * Each position in the world contains a block type ID string.
 * Uses x-then-y-then-z indexing: blocks[x][y][z]
 */
public class WorldModel {
    private static final String AIR = "air";

    private final BoundingBox3i m_box;
    private final String[][][] m_blocks;
    private final BlockTypeFactory m_blockTypeFactory;

    /**
     * Creates a new world filled with air.
     *
     * @param width The size in the X direction
     * @param height The size in the Y direction
     * @param depth The size in the Z direction (number of vertical levels)
     * @param blockTypeFactory Factory for querying block type properties
     */
    public WorldModel(int width, int height, int depth, BlockTypeFactory blockTypeFactory) {
        m_blockTypeFactory = blockTypeFactory;
        m_blocks = new String[width][height][depth];
        m_box = new BoundingBox3i(Vec3i.ZERO, new Vec3i(width, height, depth));

        // Initialize all blocks to air
        m_box.forEach(cell -> m_blocks[cell.x][cell.y][cell.z] = AIR);
    }

    /**
     * Returns the world's bounding box, anchored at {@code (0, 0, 0)}.
     * Suitable for {@code box().forEach(cell -> ...)} iteration.
     */
    public BoundingBox3i box() {
        return m_box;
    }

    /**
     * Gets the block type ID at the specified position.
     * Returns "air" if the position is out of bounds.
     */
    public String getBlock(Vec3i pos) {
        if (!isInBounds(pos)) {
            return AIR;
        }
        return m_blocks[pos.x][pos.y][pos.z];
    }

    /**
     * Sets the block at the specified position.
     * Does nothing if the position is out of bounds.
     */
    public void setBlock(Vec3i pos, String blockTypeId) {
        if (!isInBounds(pos)) {
            return;
        }
        m_blocks[pos.x][pos.y][pos.z] = blockTypeId;
    }

    /**
     * Checks if the given position is within world bounds.
     */
    public boolean isInBounds(Vec3i pos) {
        return m_box.contains(pos);
    }

    /**
     * Checks if an entity can occupy the given position.
     * Returns true if the block at pos is not solid (e.g., air).
     */
    public boolean isPassable(Vec3i pos) {
        return !isSolid(pos);
    }

    /**
     * Checks if the block at the given position is solid.
     */
    public boolean isSolid(Vec3i pos) {
        return m_blockTypeFactory.isSolid(getBlock(pos));
    }

    /**
     * Returns true if the block at the given position is air.
     */
    public boolean isAir(Vec3i pos) {
        return AIR.equals(getBlock(pos));
    }

    /**
     * Checks if there is a solid block below the given position.
     * This is used to determine if an entity has ground to stand on.
     */
    public boolean hasFloor(Vec3i pos) {
        return isSolid(pos.add(Vec3i.DOWN));
    }

    /**
     * Checks if there is a solid block above the given position.
     */
    public boolean hasCeiling(Vec3i pos) {
        return isSolid(pos.add(Vec3i.UP));
    }

    public int getWidth() {
        return m_box.size().x;
    }

    public int getHeight() {
        return m_box.size().y;
    }

    public int getDepth() {
        return m_box.size().z;
    }
}
