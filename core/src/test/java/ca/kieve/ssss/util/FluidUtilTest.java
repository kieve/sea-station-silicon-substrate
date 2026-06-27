package ca.kieve.ssss.util;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.FluidBarrier;
import ca.kieve.ssss.component.Openable;
import ca.kieve.ssss.component.Solid;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.PositionContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FluidUtilTest {
    private static final int MAX_HEIGHT = FluidUtil.MAX_BARRIER_HEIGHT;
    private static final double FULL = FluidContext.FULL;
    private static final double EPS = 1e-9;

    @Test
    void verticalFlowIsOpenUnlessBlocked() {
        assertEquals(1.0, FluidUtil.verticalConductance(FluidUtil.OPEN), EPS);
        assertEquals(0.0, FluidUtil.verticalConductance(FluidUtil.SOLID), EPS);
    }

    @Test
    void lateralFlowIsFullThroughOpenSpace() {
        assertEquals(1.0, FluidUtil.lateralConductance(0.5, FluidUtil.OPEN), EPS);
    }

    @Test
    void watertightBulkheadBlocksAllLateralFlow() {
        FluidBarrier bulkhead = new FluidBarrier(MAX_HEIGHT, FluidUtil.MAX_RESISTANCE);
        assertTrue(FluidUtil.blocksAllFlow(bulkhead));
        assertEquals(0.0, FluidUtil.lateralConductance(FULL, bulkhead), EPS);
    }

    @Test
    void shortWallContainsLowWaterButOverflowsAboveItsHeight() {
        FluidBarrier tub = new FluidBarrier(5, FluidUtil.MAX_RESISTANCE);
        assertEquals(
            1.0,
            FluidUtil.lateralConductance(0.7, tub),
            EPS,
            "water deeper than half a cell overflows a height-5 wall"
        );
        assertEquals(
            0.0,
            FluidUtil.lateralConductance(0.5, tub),
            EPS,
            "water level with the lip is contained"
        );
        assertEquals(
            0.0,
            FluidUtil.lateralConductance(0.3, tub),
            EPS,
            "water below the lip is contained"
        );
    }

    @Test
    void barrierHeightIsIndependentOfTheRenderScale() {
        FluidBarrier tallWall = new FluidBarrier(7, FluidUtil.MAX_RESISTANCE);
        assertEquals(7, tallWall.barrierHeight(), "height 7 is no longer clamped to the 0-4 scale");
        assertEquals(
            1.0,
            FluidUtil.lateralConductance(0.8, tallWall),
            EPS,
            "water deeper than 0.7 spills the height-7 wall"
        );
        assertEquals(
            0.0,
            FluidUtil.lateralConductance(0.6, tallWall),
            EPS,
            "water below 0.7 is held back by the watertight wall"
        );
    }

    @Test
    void leakyDoorSeepsAtReducedRateButWatertightDoorNeverDoes() {
        FluidBarrier leaky = new FluidBarrier(MAX_HEIGHT, 9);
        assertFalse(FluidUtil.blocksAllFlow(leaky));
        assertEquals(0.1, FluidUtil.lateralConductance(FULL, leaky), EPS);

        FluidBarrier sealed = new FluidBarrier(MAX_HEIGHT, FluidUtil.MAX_RESISTANCE);
        assertEquals(0.0, FluidUtil.lateralConductance(FULL, sealed), EPS);
    }

    @Test
    void barrierIntoResolvesWallsDoorsAndEmptyCells() {
        Dominion ecs = Dominion.create();
        PositionContext pos = new PositionContext();

        Vec3i emptyCell = new Vec3i(0, 0, 0);
        Vec3i wallCell = new Vec3i(1, 0, 0);
        Vec3i closedDoorCell = new Vec3i(2, 0, 0);
        Vec3i openDoorCell = new Vec3i(3, 0, 0);

        Entity wall = ecs.createEntity(new Solid());
        pos.add(wall, wallCell);

        Openable closed = new Openable("door_open", "door_closed");
        Entity closedDoor = ecs.createEntity(closed, new FluidBarrier(MAX_HEIGHT, 9), new Solid());
        pos.add(closedDoor, closedDoorCell);

        Openable opened = new Openable("door_open", "door_closed");
        opened.isOpen = true;
        Entity openDoor = ecs.createEntity(opened, new FluidBarrier(MAX_HEIGHT, 9), new Solid());
        pos.add(openDoor, openDoorCell);

        assertEquals(FluidUtil.OPEN, FluidUtil.barrierInto(pos, emptyCell));
        assertEquals(FluidUtil.SOLID, FluidUtil.barrierInto(pos, wallCell));
        assertEquals(new FluidBarrier(MAX_HEIGHT, 9), FluidUtil.barrierInto(pos, closedDoorCell));
        assertEquals(FluidUtil.OPEN, FluidUtil.barrierInto(pos, openDoorCell));
    }
}
