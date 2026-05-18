package ca.kieve.ssss.context;

import com.github.yellowstonegames.grid.Coord;
import dev.dominion.ecs.api.Entity;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.Openable;
import ca.kieve.ssss.testharness.TestEngine;
import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for {@link PathingContext}: same-region pathing, cross-region
 * portal selection, and per-mover portal passability (Phase 4c).
 */
class PathingContextTest {
    @Test
    void sameRegionFindsForwardStepTowardGoal() {
        TestEngine engine = TestEngine.createEmpty();
        // empty.yaml is a 9x9 walled room with air at (1..7, 1..7, 1)
        // and the player at (4, 4, 1). Path along the south wall (y=1)
        // avoids the player entirely.
        Vec3i start = new Vec3i(1, 1, 1);
        Vec3i goal = new Vec3i(7, 1, 1);

        Coord next = engine.context().pathing().findNextStep(start, goal);

        assertNotNull(next, "expected a path along the open south row");
        // Next step must be strictly closer to the goal on the east axis
        assertTrue(next.x > start.x, "expected east step, got " + next);
        // Result is in WORLD coords (not region-local — both happen to
        // coincide here since the only region is at offset (0,0,0))
        assertEquals(1, next.y);
    }

    @Test
    void sameRegionReturnsNullWhenNoPath() {
        TestEngine engine = TestEngine.createEmpty();
        // Path to a cell that's outside every region — the planner
        // can't even find a goal region.
        Vec3i start = new Vec3i(1, 1, 1);
        Vec3i unreachableGoal = new Vec3i(50, 50, 1);

        Coord next = engine.context().pathing().findNextStep(start, unreachableGoal);

        assertNull(next);
    }

    @Test
    void differentZLevelsReturnNull() {
        TestEngine engine = TestEngine.createEmpty();
        Vec3i start = new Vec3i(1, 1, 1);
        Vec3i goalDifferentZ = new Vec3i(1, 1, 0);

        Coord next = engine.context().pathing().findNextStep(start, goalDifferentZ);

        assertNull(next);
    }

    @Test
    void crossRegionStepsTowardPortal() {
        TestEngine engine = TestEngine.create("test/composite/two_rooms_open.yaml");
        // Interior air at (1, 1, 1) in room_east; goal deep in room_west.
        // Portal at (3, 1, 1) ↔ (4, 1, 1). First step should head east.
        Vec3i inEast = new Vec3i(1, 1, 1);
        Vec3i inWest = new Vec3i(6, 1, 1);

        Coord next = engine.context().pathing().findNextStep(inEast, inWest);

        assertNotNull(next, "expected a path via the boundary portal");
        assertTrue(next.x > inEast.x, "expected east step, got " + next);
        assertEquals(1, next.y);
    }

    @Test
    void crossRegionFromPortalCellStepsAcross() {
        TestEngine engine = TestEngine.create("test/composite/two_rooms_open.yaml");
        // Standing on room_east's side of the portal — next step is the
        // room_west side, one cell east.
        Vec3i portalEastSide = new Vec3i(3, 1, 1);
        Vec3i inWest = new Vec3i(6, 1, 1);

        Coord next = engine.context().pathing().findNextStep(portalEastSide, inWest);

        assertNotNull(next);
        assertEquals(4, next.x);
        assertEquals(1, next.y);
    }

    @Test
    void crossRegionReverseDirectionAlsoWorks() {
        TestEngine engine = TestEngine.create("test/composite/two_rooms_open.yaml");
        // Path from room_west back toward room_east through the portal.
        Vec3i inWest = new Vec3i(6, 1, 1);
        Vec3i inEast = new Vec3i(1, 1, 1);

        Coord next = engine.context().pathing().findNextStep(inWest, inEast);

        assertNotNull(next, "expected a path back through the portal");
        assertTrue(next.x < inWest.x, "expected west step, got " + next);
    }

    @Test
    void sizedVariantRoutesAcrossRegions() {
        TestEngine engine = TestEngine.create("test/composite/two_rooms_open.yaml");
        var mover = engine.controlledEntity();

        Vec3i inEast = new Vec3i(1, 1, 1);
        Vec3i inWest = new Vec3i(6, 1, 1);

        Coord next = engine.context().pathing().findNextStep(inEast, inWest, mover);

        assertNotNull(next);
        assertTrue(next.x > inEast.x);
    }

    @Test
    void startOutsideAnyRegionReturnsNull() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");
        // (15, 1, 1) is in the unstamped gap north of maintenance_sub —
        // inside the world bounds but not claimed by any region.
        Vec3i outside = new Vec3i(15, 1, 1);
        Vec3i insideMaintenance = new Vec3i(14, 5, 1);

        Coord next = engine.context().pathing().findNextStep(outside, insideMaintenance);

        assertNull(next);
    }

    @Test
    void lockedDoorAtPortalBlocksCrossRegion() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");
        // The only portal between the two regions is the door at
        // (11, 5, 1) and it's closed-and-locked at load time. The
        // per-mover predicate must reject it.
        Vec3i fromPortalSide = new Vec3i(11, 5, 1);
        Vec3i intoMaintenance = new Vec3i(14, 5, 1);

        Coord next = engine.context().pathing().findNextStep(fromPortalSide, intoMaintenance);

        assertNull(next, "closed/locked door must block cross-region pathing");
    }

    @Test
    void closedDoorBlocksWithinRegionPath() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");
        // damaged_sub has an interior door at (3, 5, 1). The corridor at
        // y=5 is the only east-west route; walls flank the door at
        // (3, 4) and (3, 6). With the door closed (its default), pathing
        // west to (1, 5, 1) is impossible.
        Vec3i start = new Vec3i(5, 5, 1);
        Vec3i westOfDoor = new Vec3i(1, 5, 1);

        Coord next = engine.context().pathing().findNextStep(start, westOfDoor);

        assertNull(next, "closed door must block the only westward route");
    }

    @Test
    void openDoorAllowsWithinRegionPath() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");
        openDoorAt(engine, new Vec3i(3, 5, 1));

        Vec3i start = new Vec3i(5, 5, 1);
        Vec3i westOfDoor = new Vec3i(1, 5, 1);

        Coord next = engine.context().pathing().findNextStep(start, westOfDoor);

        assertNotNull(next, "opened door must let the path through");
        assertTrue(next.x < start.x, "expected west step, got " + next);
    }

    @Test
    void openDoorAtPortalAllowsCrossRegion() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");
        openDoorAt(engine, new Vec3i(11, 5, 1));

        Vec3i fromPortalSide = new Vec3i(11, 5, 1);
        Vec3i intoMaintenance = new Vec3i(14, 5, 1);

        Coord next = engine.context().pathing().findNextStep(fromPortalSide, intoMaintenance);

        assertNotNull(next, "open door must let cross-region pathing through");
        assertEquals(12, next.x);
        assertEquals(5, next.y);
    }

    private static void openDoorAt(TestEngine engine, Vec3i pos) {
        for (Entity entity : engine.context().pos().getAt(pos)) {
            Openable openable = entity.get(Openable.class);
            if (openable != null) {
                openable.isOpen = true;
                return;
            }
        }
        throw new IllegalStateException("no openable entity at " + pos);
    }
}
