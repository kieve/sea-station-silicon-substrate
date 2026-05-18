package ca.kieve.ssss.context;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.util.Vec3i;
import ca.kieve.ssss.world.MapRegion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapContextTest {
    @Test
    void emptyContextRegionsListIsEmpty() {
        MapContext ctx = new MapContext();

        assertEquals(0, ctx.getRegions().size());
    }

    @Test
    void regionAtBeforeInitReturnsNull() {
        // Defensive lookup before the generator has run — no map loaded yet.
        MapContext ctx = new MapContext();

        assertNull(ctx.regionAt(new Vec3i(0, 0, 0)));
    }

    @Test
    void markOwnershipBeforeInitThrows() {
        MapContext ctx = new MapContext();
        MapRegion region = new MapRegion("r", Vec3i.ZERO, new Vec3i(5, 5, 3));

        assertThrows(IllegalStateException.class, () -> ctx.markOwnership(Vec3i.ZERO, region));
    }

    @Test
    void regionAtReturnsStampedRegion() {
        MapContext ctx = new MapContext();
        ctx.init(new Vec3i(5, 5, 3));
        MapRegion region = new MapRegion("only", Vec3i.ZERO, new Vec3i(5, 5, 3));
        ctx.addRegion(region);
        ctx.markOwnership(new Vec3i(2, 3, 1), region);

        assertSame(region, ctx.regionAt(new Vec3i(2, 3, 1)));
    }

    @Test
    void regionAtReturnsNullForUnstampedCellsWithinBounds() {
        // Only some cells were stamped — unstamped cells should report
        // "no owner" rather than falling back to a bounding-box answer.
        MapContext ctx = new MapContext();
        ctx.init(new Vec3i(5, 5, 3));
        MapRegion region = new MapRegion("only", Vec3i.ZERO, new Vec3i(5, 5, 3));
        ctx.addRegion(region);
        ctx.markOwnership(new Vec3i(2, 3, 1), region);

        assertNull(ctx.regionAt(new Vec3i(0, 0, 0)));
    }

    @Test
    void regionAtReturnsNullForOutOfBounds() {
        MapContext ctx = new MapContext();
        ctx.init(new Vec3i(5, 5, 3));
        MapRegion region = new MapRegion("only", Vec3i.ZERO, new Vec3i(5, 5, 3));
        ctx.addRegion(region);
        ctx.markOwnership(Vec3i.ZERO, region);

        assertNull(ctx.regionAt(new Vec3i(5, 0, 0)));
        assertNull(ctx.regionAt(new Vec3i(-1, 0, 0)));
        assertNull(ctx.regionAt(new Vec3i(0, 0, 3)));
    }

    @Test
    void cellStampsDistinguishOverlappingRegionBoundingBoxes() {
        // The whole reason the cell-stamp model replaced bounding-box
        // lookup: when two regions' boxes overlap, the stamp must answer
        // with whoever actually claimed each cell, not "whichever box
        // contains the point first."
        MapContext ctx = new MapContext();
        ctx.init(new Vec3i(10, 10, 3));
        MapRegion a = new MapRegion("a", Vec3i.ZERO, new Vec3i(5, 5, 3));
        MapRegion b = new MapRegion("b", new Vec3i(3, 0, 0), new Vec3i(5, 5, 3));
        ctx.addRegion(a);
        ctx.addRegion(b);
        // Cell at (4, 1, 1) is inside both bounding boxes, but b claimed it.
        ctx.markOwnership(new Vec3i(4, 1, 1), b);

        assertSame(b, ctx.regionAt(new Vec3i(4, 1, 1)));
    }

    @Test
    void getRegionsIsUnmodifiable() {
        MapContext ctx = new MapContext();
        ctx.addRegion(new MapRegion("only", Vec3i.ZERO, new Vec3i(5, 5, 3)));

        var view = ctx.getRegions();

        assertEquals(1, view.size());
        assertThrows(
            UnsupportedOperationException.class,
            () -> view.add(new MapRegion("nope", Vec3i.ZERO, new Vec3i(1, 1, 1)))
        );
    }
}
