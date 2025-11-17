package ca.kieve.ssss.world;

/**
 * Represents the type of material a block is made of.
 * In a voxel world, every position contains a block of some type.
 * AIR represents empty space that entities can occupy.
 */
public enum BlockType {
    AIR(false, false),
    STONE(true, true),
    WOOD(true, true),
    STEEL(true, true);

    private final boolean m_solid;
    private final boolean m_opaque;

    BlockType(boolean solid, boolean opaque) {
        m_solid = solid;
        m_opaque = opaque;
    }

    /**
     * Returns true if entities cannot pass through this block type.
     */
    public boolean isSolid() {
        return m_solid;
    }

    /**
     * Returns true if this block type blocks light/vision.
     */
    public boolean isOpaque() {
        return m_opaque;
    }
}
