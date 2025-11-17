package ca.kieve.ssss.world;

/**
 * Represents the data for a single block in the voxel world.
 * This is a lightweight data structure that stores the block's type
 * and any future metadata (temperature, pressure, fluid level, etc.).
 */
public record BlockData(BlockType type) {

    /**
     * Convenience constant for air blocks.
     */
    public static final BlockData AIR = new BlockData(BlockType.AIR);

    /**
     * Convenience constant for stone blocks.
     */
    public static final BlockData STONE = new BlockData(BlockType.STONE);

    /**
     * Convenience constant for wood blocks.
     */
    public static final BlockData WOOD = new BlockData(BlockType.WOOD);

    /**
     * Convenience constant for steel blocks.
     */
    public static final BlockData STEEL = new BlockData(BlockType.STEEL);

    /**
     * Returns true if this block is solid (entities cannot pass through).
     */
    public boolean isSolid() {
        return type.isSolid();
    }

    /**
     * Returns true if this block is opaque (blocks light/vision).
     */
    public boolean isOpaque() {
        return type.isOpaque();
    }

    /**
     * Returns true if this is an air block.
     */
    public boolean isAir() {
        return type == BlockType.AIR;
    }
}
