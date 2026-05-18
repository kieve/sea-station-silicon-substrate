package ca.kieve.ssss.world;

import ca.kieve.ssss.util.Require;
import ca.kieve.ssss.util.Vec3i;

/**
 * One topological connection between two {@link MapRegion}s. A portal
 * pairs a passable cell on each side of a region boundary; an entity in
 * {@link #cellInA} can step directly to {@link #cellInB} (and vice versa)
 * to cross from {@link #regionA} into {@link #regionB}.
 *
 * <p>"Passable" here means non-solid in the static {@link WorldModel} —
 * dynamic obstructions (closed doors, NPCs) at the portal cell don't
 * change whether the portal exists, only whether it's currently
 * traversable for a given mover (Phase 4c).
 */
public record Portal(String regionA, Vec3i cellInA, String regionB, Vec3i cellInB) {
    public Portal {
        Require.componentsNonNull(regionA, cellInA, regionB, cellInB);
    }

    /** Returns the region on the opposite side of this portal from {@code thisRegion}. */
    public String otherRegion(String thisRegion) {
        if (regionA.equals(thisRegion)) {
            return regionB;
        }
        if (regionB.equals(thisRegion)) {
            return regionA;
        }
        throw new IllegalArgumentException(
            "portal between " + regionA + " and " + regionB + " is not connected to " + thisRegion
        );
    }

    /** Returns this portal's cell in the named region. */
    public Vec3i cellIn(String region) {
        if (regionA.equals(region)) {
            return cellInA;
        }
        if (regionB.equals(region)) {
            return cellInB;
        }
        throw new IllegalArgumentException(
            "portal between " + regionA + " and " + regionB + " is not connected to " + region
        );
    }
}
