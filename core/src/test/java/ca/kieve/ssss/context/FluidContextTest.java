package ca.kieve.ssss.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidContextTest {
    private static final double EPS = 1e-9;

    private FluidContext fluid;

    @BeforeEach
    void setUp() {
        fluid = new FluidContext();
        fluid.init(new Vec3i(4, 4, 2));
    }

    @Test
    void setLevelRoundTripsThroughGetLevel() {
        Vec3i pos = new Vec3i(1, 2, 0);
        for (int level = 0; level <= FluidContext.MAX_LEVEL; level++) {
            fluid.setLevel(pos, level);
            assertEquals(level, fluid.getLevel(pos), "round trip for level " + level);
        }
    }

    @Test
    void massQuantizesIntoGlyphBands() {
        Vec3i pos = new Vec3i(0, 0, 0);

        fluid.setMass(pos, 0.0);
        assertEquals(0, fluid.getLevel(pos));
        fluid.setMass(pos, 0.05);
        assertEquals(1, fluid.getLevel(pos));
        fluid.setMass(pos, 0.10);
        assertEquals(1, fluid.getLevel(pos));
        fluid.setMass(pos, 0.30);
        assertEquals(2, fluid.getLevel(pos));
        fluid.setMass(pos, 0.55);
        assertEquals(2, fluid.getLevel(pos));
        fluid.setMass(pos, 0.70);
        assertEquals(3, fluid.getLevel(pos));
        fluid.setMass(pos, 1.0);
        assertEquals(4, fluid.getLevel(pos));
        fluid.setMass(pos, 2.5);
        assertEquals(4, fluid.getLevel(pos), "over-compressed water still reads as full");
    }

    @Test
    void setMassClampsNegativeToZero() {
        Vec3i pos = new Vec3i(0, 0, 0);

        fluid.setMass(pos, -4);
        assertEquals(0.0, fluid.getMass(pos), EPS);
        assertFalse(fluid.hasWater(pos));
    }

    @Test
    void sourcesTrackPressureDepth() {
        Vec3i pos = new Vec3i(2, 2, 1);

        fluid.setSource(pos, 100);
        assertTrue(fluid.isSource(pos));
        assertEquals(100.0, fluid.sourceDepth(pos), EPS);
        assertTrue(fluid.sourceCells().contains(pos));

        fluid.setSource(pos, 0);
        assertFalse(fluid.isSource(pos));
        assertTrue(fluid.sourceCells().isEmpty());
    }

    @Test
    void totalVolumeSumsAllMass() {
        assertEquals(0.0, fluid.totalVolume(), EPS);

        fluid.setMass(new Vec3i(0, 0, 0), 1.0);
        fluid.setMass(new Vec3i(1, 1, 1), 0.5);
        fluid.addMass(new Vec3i(2, 2, 0), 0.25);

        assertEquals(1.75, fluid.totalVolume(), EPS);
    }

    @Test
    void waterCellsListsOnlyWetCells() {
        fluid.setMass(new Vec3i(0, 0, 0), 1.0);
        fluid.setMass(new Vec3i(3, 3, 1), 0.05);

        var cells = fluid.waterCells();

        assertEquals(2, cells.size());
        assertTrue(cells.contains(new Vec3i(0, 0, 0)));
        assertTrue(cells.contains(new Vec3i(3, 3, 1)));
    }

    @Test
    void outOfBoundsReadsZeroAndWritesAreNoOps() {
        Vec3i oob = new Vec3i(99, 99, 99);

        assertEquals(0.0, fluid.getMass(oob), EPS);
        fluid.setMass(oob, 1.0);
        fluid.addMass(oob, 1.0);
        fluid.setSource(oob, 5);
        assertEquals(0.0, fluid.getMass(oob), EPS);
        assertFalse(fluid.isSource(oob));
        assertEquals(0.0, fluid.totalVolume(), EPS);
    }

    @Test
    void beforeInitReadsZeroAndWritesAreNoOps() {
        FluidContext uninit = new FluidContext();

        assertEquals(0.0, uninit.getMass(new Vec3i(0, 0, 0)), EPS);
        assertEquals(0.0, uninit.totalVolume(), EPS);
        assertTrue(uninit.waterCells().isEmpty());

        uninit.setMass(new Vec3i(0, 0, 0), 1.0);
        assertEquals(0.0, uninit.getMass(new Vec3i(0, 0, 0)), EPS);
    }
}
