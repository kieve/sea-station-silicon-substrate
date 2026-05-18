package ca.kieve.ssss.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundingBox3iTest {
    @Test
    void containsOriginWhenOriginIsZero() {
        BoundingBox3i box = new BoundingBox3i(Vec3i.ZERO, new Vec3i(5, 5, 3));

        assertTrue(box.contains(new Vec3i(0, 0, 0)));
    }

    @Test
    void containsPointStrictlyInside() {
        BoundingBox3i box = new BoundingBox3i(Vec3i.ZERO, new Vec3i(5, 5, 3));

        assertTrue(box.contains(new Vec3i(2, 3, 1)));
    }

    @Test
    void upperEdgeIsExclusive() {
        BoundingBox3i box = new BoundingBox3i(Vec3i.ZERO, new Vec3i(5, 5, 3));

        assertFalse(box.contains(new Vec3i(5, 0, 0)));
        assertFalse(box.contains(new Vec3i(0, 5, 0)));
        assertFalse(box.contains(new Vec3i(0, 0, 3)));
    }

    @Test
    void negativeCoordinatesExcluded() {
        BoundingBox3i box = new BoundingBox3i(Vec3i.ZERO, new Vec3i(5, 5, 3));

        assertFalse(box.contains(new Vec3i(-1, 0, 0)));
        assertFalse(box.contains(new Vec3i(0, -1, 0)));
        assertFalse(box.contains(new Vec3i(0, 0, -1)));
    }

    @Test
    void originShiftsContainsCheck() {
        BoundingBox3i box = new BoundingBox3i(new Vec3i(10, 20, 1), new Vec3i(5, 5, 2));

        assertTrue(box.contains(new Vec3i(10, 20, 1)));
        assertTrue(box.contains(new Vec3i(14, 24, 2)));
        assertFalse(box.contains(new Vec3i(9, 20, 1)));
        assertFalse(box.contains(new Vec3i(0, 0, 0)));
    }

    @Test
    void constructorDefensiveCopiesOrigin() {
        Vec3i origin = new Vec3i(1, 2, 3);
        BoundingBox3i box = new BoundingBox3i(origin, new Vec3i(5, 5, 3));

        origin.x = 999;

        assertNotSame(origin, box.origin());
        assertEquals(1, box.origin().x);
    }

    @Test
    void constructorDefensiveCopiesSize() {
        Vec3i size = new Vec3i(5, 5, 3);
        BoundingBox3i box = new BoundingBox3i(Vec3i.ZERO, size);

        size.x = 999;

        assertNotSame(size, box.size());
        assertEquals(5, box.size().x);
        assertFalse(box.contains(new Vec3i(5, 0, 0)));
    }

    @Test
    void forEachVisitsEveryCellOnce() {
        BoundingBox3i box = new BoundingBox3i(new Vec3i(10, 20, 1), new Vec3i(2, 3, 2));

        Set<Vec3i> visited = new HashSet<>();
        box.forEach(cell -> {
            // Must not see the same cell twice
            boolean fresh = visited.add(cell);
            Assertions.assertTrue(fresh, "duplicate visit at " + cell);
        });

        // 2 × 3 × 2 = 12 cells, every one in [origin, origin+size)
        assertEquals(12, visited.size());
        assertTrue(visited.contains(new Vec3i(10, 20, 1)));
        assertTrue(visited.contains(new Vec3i(11, 22, 2)));
        // Upper edges are exclusive
        Assertions.assertFalse(visited.contains(new Vec3i(12, 20, 1)));
        Assertions.assertFalse(visited.contains(new Vec3i(10, 23, 1)));
        Assertions.assertFalse(visited.contains(new Vec3i(10, 20, 3)));
    }

    @Test
    void zeroOrNegativeSizeRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new BoundingBox3i(Vec3i.ZERO, new Vec3i(0, 5, 3))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new BoundingBox3i(Vec3i.ZERO, new Vec3i(5, 0, 3))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new BoundingBox3i(Vec3i.ZERO, new Vec3i(5, 5, 0))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new BoundingBox3i(Vec3i.ZERO, new Vec3i(-1, 5, 3))
        );
    }
}
