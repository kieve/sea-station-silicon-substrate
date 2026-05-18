package ca.kieve.ssss.world;

import ca.kieve.ssss.util.BoundingBox3i;
import ca.kieve.ssss.util.Require;
import ca.kieve.ssss.util.Vec3i;

/**
 * A rectangular region of the world that came from a single source map YAML.
 *
 * <p>Phase 1: there is exactly one region per loaded world, covering the
 * entire {@link WorldModel}. Future phases introduce nested/composed maps
 * where multiple regions coexist with distinct offsets.
 *
 * <p>{@link #box} captures where the region sits in world coordinates:
 * its origin is the region's {@code (0,0,0)} in world space, and its size
 * is the region's extent on each axis.
 */
public record MapRegion(String id, BoundingBox3i box) {
    public MapRegion {
        Require.componentsNonNull(id, box);
    }

    /**
     * Convenience constructor for the common case of building a region from
     * origin + size directly.
     */
    public MapRegion(String id, Vec3i originOffset, Vec3i bounds) {
        this(id, new BoundingBox3i(originOffset, bounds));
    }

    /**
     * Returns true if {@code worldPos} falls inside this region's bounding box.
     */
    public boolean contains(Vec3i worldPos) {
        return box.contains(worldPos);
    }
}
