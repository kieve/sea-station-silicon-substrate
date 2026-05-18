package ca.kieve.ssss.util;

import java.util.function.Consumer;

/**
 * A half-open axis-aligned bounding box in integer 3D space.
 *
 * <p>The lower edge ({@link #origin}) is inclusive; the upper edge
 * ({@code origin + size}) is exclusive. {@link #size} must be positive on
 * every axis.
 */
public record BoundingBox3i(Vec3i origin, Vec3i size) {
    public BoundingBox3i {
        Require.componentsNonNull(origin, size);
        if (size.x <= 0 || size.y <= 0 || size.z <= 0) {
            throw new IllegalArgumentException(
                "BoundingBox3i size must be positive on all axes: " + size
            );
        }
        // Defensive copy so later mutation of the caller's Vec3i can't shift the box.
        origin = origin.copy();
        size = size.copy();
    }

    /**
     * Returns true if {@code point} lies in {@code [origin, origin + size)}.
     */
    public boolean contains(Vec3i point) {
        int rx = point.x - origin.x;
        int ry = point.y - origin.y;
        int rz = point.z - origin.z;
        return rx >= 0 && rx < size.x
            && ry >= 0 && ry < size.y
            && rz >= 0 && rz < size.z;
    }

    /**
     * Iterates every integer cell in the box, invoking {@code action} with
     * a fresh {@link Vec3i} each time. Iteration order: x outermost, then
     * y, then z innermost (matches {@code arr[x][y][z]} cache locality).
     *
     * <p>Each call allocates a new {@link Vec3i}, so the lambda may
     * safely retain references.
     */
    public void forEach(Consumer<Vec3i> action) {
        int xEnd = origin.x + size.x;
        int yEnd = origin.y + size.y;
        int zEnd = origin.z + size.z;
        for (int x = origin.x; x < xEnd; x++) {
            for (int y = origin.y; y < yEnd; y++) {
                for (int z = origin.z; z < zEnd; z++) {
                    action.accept(new Vec3i(x, y, z));
                }
            }
        }
    }
}
