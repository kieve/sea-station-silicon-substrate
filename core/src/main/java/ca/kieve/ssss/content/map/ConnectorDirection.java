package ca.kieve.ssss.content.map;

import ca.kieve.ssss.util.Vec3i;

/**
 * Compass-style facing for a connector. Used in connector-mode submap
 * placement: a parent connector with {@code direction: east} places the
 * docking submap so its remote connector lands one cell <em>east</em> of
 * the parent connector — the two doors end up adjacent rather than
 * sharing a tile.
 *
 * <p>Matches the project's Y-up coordinate convention (see CLAUDE.md):
 * X+ = east, Y+ = north, Z+ = up.
 */
public enum ConnectorDirection {
    EAST(Vec3i.EAST),
    WEST(Vec3i.WEST),
    NORTH(Vec3i.NORTH),
    SOUTH(Vec3i.SOUTH),
    UP(Vec3i.UP),
    DOWN(Vec3i.DOWN);

    private final Vec3i m_unitVector;

    ConnectorDirection(Vec3i unitVector) {
        m_unitVector = unitVector.copy();
    }

    public Vec3i unitVector() {
        return m_unitVector.copy();
    }
}
