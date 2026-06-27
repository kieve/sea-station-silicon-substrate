package ca.kieve.ssss.system;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.FluidBarrier;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.system.FluidSimulator.BarrierResolver;
import ca.kieve.ssss.util.FluidUtil;
import ca.kieve.ssss.util.Vec3i;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidSimulatorTest {
    private static final double FULL = FluidContext.FULL;
    private static final double EPS = 1e-6;
    private static final BarrierResolver OPEN = cell -> FluidUtil.OPEN;

    private static FluidContext fluid(int w, int h, int d) {
        FluidContext f = new FluidContext();
        f.init(new Vec3i(w, h, d));
        return f;
    }

    @Test
    void gravityCollapsesAColumnOntoTheWorldFloor() {
        FluidContext f = fluid(1, 1, 3);
        f.setMass(new Vec3i(0, 0, 2), FULL);

        for (int step = 0; step < 50; step++) {
            FluidSimulator.step(f, OPEN);
            assertEquals(FULL, f.totalVolume(), EPS, "gravity must conserve volume");
        }

        assertEquals(FULL, f.getMass(new Vec3i(0, 0, 0)), EPS, "water settles on the floor");
        assertEquals(0.0, f.getMass(new Vec3i(0, 0, 2)), EPS, "top cell drains");
    }

    @Test
    void lateralFlowSpreadsAcrossTheWholeRowWithoutStalling() {
        FluidContext f = fluid(5, 1, 1);
        f.setMass(new Vec3i(2, 0, 0), FULL);

        Set<Integer> everWet = new HashSet<>();
        for (int step = 0; step < 200; step++) {
            FluidSimulator.step(f, OPEN);
            assertEquals(FULL, f.totalVolume(), EPS, "lateral flow must conserve volume");
            for (int x = 0; x < 5; x++) {
                if (f.getMass(new Vec3i(x, 0, 0)) > 0) {
                    everWet.add(x);
                }
            }
        }

        for (int x = 0; x < 5; x++) {
            assertTrue(everWet.contains(x), "water never reached column " + x);
        }
    }

    @Test
    void aClingingFilmStaysPutInsteadOfWandering() {
        FluidContext f = fluid(5, 1, 1);
        f.setMass(new Vec3i(2, 0, 0), 0.10);

        for (int step = 0; step < 50; step++) {
            FluidSimulator.step(f, OPEN);
        }

        assertEquals(0.10, f.getMass(new Vec3i(2, 0, 0)), EPS, "a clinging film doesn't detach");
        assertEquals(0.0, f.getMass(new Vec3i(1, 0, 0)), EPS, "it never spreads");
        assertEquals(0.10, f.totalVolume(), EPS);
    }

    @Test
    void fullTilesConserveTheirVolumeWhileSpreading() {
        FluidContext f = fluid(4, 4, 1);
        f.setMass(new Vec3i(0, 0, 0), FULL);
        f.setMass(new Vec3i(1, 0, 0), FULL);
        f.setMass(new Vec3i(0, 1, 0), FULL);
        f.setMass(new Vec3i(1, 1, 0), FULL);
        assertEquals(4 * FULL, f.totalVolume(), EPS);

        for (int step = 0; step < 300; step++) {
            FluidSimulator.step(f, OPEN);
            assertEquals(4 * FULL, f.totalVolume(), EPS, "water is never created or lost");
        }
    }

    @Test
    void solidWallStopsWaterFromCrossing() {
        FluidContext f = fluid(3, 1, 1);
        Vec3i source = new Vec3i(0, 0, 0);
        Vec3i wall = new Vec3i(1, 0, 0);
        Vec3i beyond = new Vec3i(2, 0, 0);
        f.setMass(source, FULL);

        BarrierResolver walled = cell -> cell.equals(wall) ? FluidUtil.SOLID : FluidUtil.OPEN;
        for (int step = 0; step < 30; step++) {
            FluidSimulator.step(f, walled);
            assertEquals(FULL, f.totalVolume(), EPS);
        }

        assertEquals(0.0, f.getMass(beyond), EPS, "water never crosses the wall");
        assertEquals(0.0, f.getMass(wall), EPS, "water cannot enter a solid wall cell");
        assertEquals(FULL, f.getMass(source), EPS, "contained water stays put");
    }

    @Test
    void openingABulkheadFloodsTheNeighbouringRoom() {
        FluidContext f = fluid(3, 1, 1);
        Vec3i room = new Vec3i(0, 0, 0);
        Vec3i bulkhead = new Vec3i(1, 0, 0);
        Vec3i neighbour = new Vec3i(2, 0, 0);
        f.setMass(room, FULL);

        BarrierResolver closed = cell -> cell.equals(bulkhead) ? FluidUtil.SOLID : FluidUtil.OPEN;
        for (int step = 0; step < 20; step++) {
            FluidSimulator.step(f, closed);
        }
        assertEquals(0.0, f.getMass(neighbour), EPS, "sealed bulkhead holds the water back");
        assertEquals(FULL, f.getMass(room), EPS);

        for (int step = 0; step < 80; step++) {
            FluidSimulator.step(f, OPEN);
            assertEquals(FULL, f.totalVolume(), EPS);
        }
        assertTrue(f.getMass(neighbour) > 0, "opened bulkhead floods the neighbour");
    }

    @Test
    void leakyDoorSeepsThroughButWatertightDoorNeverDoes() {
        Vec3i a = new Vec3i(0, 0, 0);
        Vec3i b = new Vec3i(1, 0, 0);

        FluidContext leaky = fluid(2, 1, 1);
        leaky.setMass(a, FULL);
        FluidBarrier door = new FluidBarrier(FluidUtil.MAX_BARRIER_HEIGHT, 9);
        BarrierResolver leakResolver = cell -> cell.equals(b) ? door : FluidUtil.OPEN;
        for (int step = 0; step < 300; step++) {
            FluidSimulator.step(leaky, leakResolver);
            assertEquals(FULL, leaky.totalVolume(), EPS);
        }
        assertTrue(leaky.getMass(b) > 0, "water seeps through over time");
        assertTrue(leaky.getMass(a) < FULL, "source level falls as it seeps");

        FluidContext sealed = fluid(2, 1, 1);
        sealed.setMass(a, FULL);
        FluidBarrier bulkhead = new FluidBarrier(
            FluidUtil.MAX_BARRIER_HEIGHT,
            FluidUtil.MAX_RESISTANCE
        );
        BarrierResolver sealResolver = cell -> cell.equals(b) ? bulkhead : FluidUtil.OPEN;
        for (int step = 0; step < 300; step++) {
            FluidSimulator.step(sealed, sealResolver);
        }
        assertEquals(0.0, sealed.getMass(b), EPS, "watertight bulkhead never leaks");
        assertEquals(FULL, sealed.getMass(a), EPS);
    }

    @Test
    void equalizingWaterNeverSloshesAboveFull() {
        FluidContext f = fluid(5, 5, 1);
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                f.setMass(new Vec3i(x, y, 0), FULL);
            }
        }
        double initial = 9 * FULL;

        for (int step = 0; step < 150; step++) {
            FluidSimulator.step(f, OPEN);
            assertEquals(initial, f.totalVolume(), EPS, "water is never created or lost");
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    assertTrue(
                        f.getMass(new Vec3i(x, y, 0)) <= FULL + 1e-6,
                        "no cell may slosh above full while spreading"
                    );
                }
            }
        }
    }

    @Test
    void aPressurizedSourceFloodsOutwardAndRisesUpward() {
        FluidContext f = fluid(3, 1, 3);
        Vec3i source = new Vec3i(0, 0, 0);
        f.setSource(source, 5.0);

        for (int step = 0; step < 60; step++) {
            FluidSimulator.step(f, OPEN);
        }

        assertTrue(f.getMass(source) >= FULL, "an infinite source never depletes");
        assertTrue(f.getMass(new Vec3i(2, 0, 0)) > 0, "pressure floods across the floor");
        assertTrue(f.getMass(new Vec3i(0, 0, 1)) > 0, "pressure drives water upward");
    }
}
