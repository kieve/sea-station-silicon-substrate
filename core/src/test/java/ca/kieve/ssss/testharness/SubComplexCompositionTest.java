package ca.kieve.ssss.testharness;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.context.MapContext;
import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.util.SolidUtil;
import ca.kieve.ssss.util.Vec3i;
import ca.kieve.ssss.world.MapRegion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end smoke for the home_base/sub_complex composition: loads it
 * through the real engine, asserts both leaf regions register at the
 * expected world offsets, and walks the player east a few unblocked
 * steps to confirm the WASD/clock pipeline works against a composed
 * world.
 *
 * <p>The boundary cell (11,5,1) is a locked door so we don't actually
 * try to walk through; cross-region traversal is verified via
 * {@link MapContext#regionAt} at known cells on either side.
 */
class SubComplexCompositionTest {
    @Test
    void compositionRegistersBothLeafRegions() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");
        MapContext map = engine.context().map();

        assertEquals(2, map.getRegions().size(), map.getRegions().toString());
        var ids = map.getRegions().stream().map(MapRegion::id).toList();
        assertTrue(ids.contains("damaged_sub_root"), ids.toString());
        assertTrue(ids.contains("maintenance_east"), ids.toString());
    }

    @Test
    void regionAtDamagedSubInteriorReturnsDamagedSub() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");

        // player spawn (7,5,1) is squarely inside damaged_sub
        MapRegion region = engine.context().map().regionAt(new Vec3i(7, 5, 1));
        assertNotNull(region);
        assertEquals("damaged_sub_root", region.id());
    }

    @Test
    void regionAtMaintenanceSubInteriorReturnsMaintenanceSub() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");

        // (13,5,1) is well inside maintenance_sub (offset (11,2,0), local (2,3,1))
        MapRegion region = engine.context().map().regionAt(new Vec3i(13, 5, 1));
        assertNotNull(region);
        assertEquals("maintenance_east", region.id());
    }

    @Test
    void boundaryCellsAreAdjacentNotOverlapping() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");

        // The east_door connector faces east, so the two maps end up
        // side-by-side at x=11/x=12 instead of sharing a tile.
        MapRegion doorCell = engine.context().map().regionAt(new Vec3i(11, 5, 1));
        MapRegion oneEast = engine.context().map().regionAt(new Vec3i(12, 5, 1));
        assertNotNull(doorCell);
        assertNotNull(oneEast);
        assertEquals("damaged_sub_root", doorCell.id());
        assertEquals("maintenance_east", oneEast.id());
    }

    @Test
    void wallCellsOnRegionBoundaryHaveBlockEntities() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");

        // damaged_sub's east column at world x=11 is all walls except the
        // door at y=5. With maintenance_sub placed immediately east, the
        // wall cells (11, 2, 1) and (11, 8, 1) are surrounded by other
        // wall cells (damaged_sub west, north, south + maintenance_sub
        // east). They must still spawn block entities — otherwise nothing
        // renders at those positions and the wall appears to disappear.
        Vec3i corner1 = new Vec3i(11, 2, 1);
        Vec3i corner2 = new Vec3i(11, 8, 1);

        assertTrue(
            SolidUtil.hasWall(engine.context(), corner1),
            "expected a solid wall entity at " + corner1
        );
        assertTrue(
            SolidUtil.hasWall(engine.context(), corner2),
            "expected a solid wall entity at " + corner2
        );
    }

    @Test
    void wallCellsOnMaintenanceSideOfBoundaryHaveBlockEntities() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");

        // Mirror of the previous test: maintenance_sub's west wall at
        // world (12, 2, 1) etc. is adjacent to damaged_sub's east wall.
        // Those cells must also spawn entities.
        Vec3i corner1 = new Vec3i(12, 2, 1);
        Vec3i corner2 = new Vec3i(12, 8, 1);

        assertTrue(
            SolidUtil.hasWall(engine.context(), corner1),
            "expected a solid wall entity at " + corner1
        );
        assertTrue(
            SolidUtil.hasWall(engine.context(), corner2),
            "expected a solid wall entity at " + corner2
        );
    }

    @Test
    void playerWalksEastWithinDamagedSub() {
        TestEngine engine = TestEngine.create("home_base/sub_complex.yaml");

        Vec3i start = engine.controlledPosition();
        assertEquals(7, start.x);
        assertEquals(5, start.y);

        // Walk east three steps to (10,5,1) — last cell before the locked door.
        for (int i = 0; i < 3; i++) {
            engine.pressAction(InputAction.RIGHT);
            engine.tickTurn();
        }

        Vec3i end = engine.controlledPosition();
        assertEquals(10, end.x);
        assertEquals(5, end.y);

        // Confirm we're still inside damaged_sub at this point.
        MapRegion region = engine.context().map().regionAt(end);
        assertNotNull(region);
        assertEquals("damaged_sub_root", region.id());
    }
}
