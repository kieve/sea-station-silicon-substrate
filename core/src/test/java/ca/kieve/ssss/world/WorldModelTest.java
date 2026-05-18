package ca.kieve.ssss.world;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.content.BlockTypeFactory;
import ca.kieve.ssss.content.ContentLoader;
import ca.kieve.ssss.testharness.HeadlessGdxBootstrap;
import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldModelTest {
    private static final String AIR = "air";
    private static final String SOLID = "block_stone";

    private static BlockTypeFactory s_blocks;

    @BeforeAll
    static void loadContent() {
        HeadlessGdxBootstrap.ensureInitialized();
        s_blocks = new ContentLoader().loadAll().getBlockTypeFactory();
    }

    @Test
    void newWorldIsAirEverywhere() {
        WorldModel world = new WorldModel(3, 4, 2, s_blocks);

        world.box().forEach(cell -> assertTrue(world.isAir(cell), "expected air at " + cell));
    }

    @Test
    void dimensionsAndBoxMatchConstructor() {
        WorldModel world = new WorldModel(3, 4, 2, s_blocks);

        assertEquals(3, world.getWidth());
        assertEquals(4, world.getHeight());
        assertEquals(2, world.getDepth());
        assertEquals(Vec3i.ZERO, world.box().origin());
        assertEquals(new Vec3i(3, 4, 2), world.box().size());
    }

    @Test
    void setBlockThenGetBlockRoundTrips() {
        WorldModel world = new WorldModel(3, 3, 2, s_blocks);

        world.setBlock(new Vec3i(1, 2, 0), SOLID);

        assertEquals(SOLID, world.getBlock(new Vec3i(1, 2, 0)));
    }

    @Test
    void getBlockOutOfBoundsReturnsAir() {
        WorldModel world = new WorldModel(2, 2, 1, s_blocks);

        // Even after stamping interior cells, out-of-bounds queries still
        // report air rather than throwing — callers can probe freely.
        world.setBlock(new Vec3i(0, 0, 0), SOLID);

        assertEquals(AIR, world.getBlock(new Vec3i(2, 0, 0)));
        assertEquals(AIR, world.getBlock(new Vec3i(0, 2, 0)));
        assertEquals(AIR, world.getBlock(new Vec3i(0, 0, 1)));
        assertEquals(AIR, world.getBlock(new Vec3i(-1, 0, 0)));
    }

    @Test
    void setBlockOutOfBoundsIsNoOp() {
        WorldModel world = new WorldModel(2, 2, 1, s_blocks);

        world.setBlock(new Vec3i(5, 5, 5), SOLID);

        // Nothing should change anywhere inside the grid.
        world.box().forEach(cell -> assertEquals(AIR, world.getBlock(cell)));
    }

    @Test
    void isInBoundsMatchesBoxContains() {
        WorldModel world = new WorldModel(3, 3, 2, s_blocks);

        assertTrue(world.isInBounds(new Vec3i(0, 0, 0)));
        assertTrue(world.isInBounds(new Vec3i(2, 2, 1)));
        assertFalse(world.isInBounds(new Vec3i(3, 0, 0)));
        assertFalse(world.isInBounds(new Vec3i(0, 0, 2)));
        assertFalse(world.isInBounds(new Vec3i(-1, 0, 0)));
    }

    @Test
    void isSolidDelegatesToBlockTypeFactory() {
        WorldModel world = new WorldModel(2, 2, 1, s_blocks);
        Vec3i cell = new Vec3i(0, 0, 0);

        // Air → not solid
        assertFalse(world.isSolid(cell));

        world.setBlock(cell, SOLID);
        assertTrue(world.isSolid(cell));
    }

    @Test
    void isPassableIsInverseOfIsSolid() {
        WorldModel world = new WorldModel(2, 2, 1, s_blocks);
        Vec3i cell = new Vec3i(0, 0, 0);

        assertTrue(world.isPassable(cell));

        world.setBlock(cell, SOLID);
        assertFalse(world.isPassable(cell));
    }

    @Test
    void outOfBoundsIsPassable() {
        // OOB cells read as air, so they're "passable" — relied on by
        // anything that probes neighbours near world edges.
        WorldModel world = new WorldModel(2, 2, 1, s_blocks);

        assertTrue(world.isPassable(new Vec3i(5, 5, 5)));
        assertFalse(world.isSolid(new Vec3i(5, 5, 5)));
    }

    @Test
    void hasFloorReadsCellBelow() {
        WorldModel world = new WorldModel(3, 3, 3, s_blocks);
        Vec3i standOn = new Vec3i(1, 1, 1);

        // No floor yet — the cell below is air.
        assertFalse(world.hasFloor(standOn));

        world.setBlock(standOn.add(Vec3i.DOWN), SOLID);
        assertTrue(world.hasFloor(standOn));
    }

    @Test
    void hasCeilingReadsCellAbove() {
        WorldModel world = new WorldModel(3, 3, 3, s_blocks);
        Vec3i pos = new Vec3i(1, 1, 1);

        assertFalse(world.hasCeiling(pos));

        world.setBlock(pos.add(Vec3i.UP), SOLID);
        assertTrue(world.hasCeiling(pos));
    }

    @Test
    void hasFloorAtZeroZIsFalse() {
        // The cell below (z = -1) is OOB → air → not solid.
        WorldModel world = new WorldModel(2, 2, 2, s_blocks);

        assertFalse(world.hasFloor(new Vec3i(0, 0, 0)));
    }
}
