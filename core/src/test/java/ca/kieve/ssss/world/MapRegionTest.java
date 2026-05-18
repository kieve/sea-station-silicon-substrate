package ca.kieve.ssss.world;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.util.BoundingBox3i;
import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapRegionTest {
    @Test
    void containsDelegatesToBox() {
        MapRegion region = new MapRegion(
            "r",
            new BoundingBox3i(new Vec3i(10, 0, 0), new Vec3i(5, 5, 3))
        );

        assertTrue(region.contains(new Vec3i(10, 0, 0)));
        assertTrue(region.contains(new Vec3i(14, 4, 2)));
        assertFalse(region.contains(new Vec3i(15, 0, 0)));
        assertFalse(region.contains(new Vec3i(0, 0, 0)));
    }

    @Test
    void convenienceConstructorBuildsBoxFromOriginAndBounds() {
        MapRegion region = new MapRegion("r", new Vec3i(1, 2, 3), new Vec3i(4, 5, 6));

        assertEquals(new Vec3i(1, 2, 3), region.box().origin());
        assertEquals(new Vec3i(4, 5, 6), region.box().size());
    }

    @Test
    void nullIdRejected() {
        assertThrows(
            NullPointerException.class,
            () -> new MapRegion(null, new BoundingBox3i(Vec3i.ZERO, new Vec3i(1, 1, 1)))
        );
    }

    @Test
    void nullBoxRejected() {
        assertThrows(NullPointerException.class, () -> new MapRegion("r", null));
    }
}
