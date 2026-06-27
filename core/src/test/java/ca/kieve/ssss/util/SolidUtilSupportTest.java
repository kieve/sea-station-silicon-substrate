package ca.kieve.ssss.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.testharness.TestGameContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolidUtilSupportTest {
    private GameContext context;

    @BeforeEach
    void setUp() {
        context = TestGameContext.createEmpty();
    }

    @Test
    void solidBlockBelowSupports() {
        assertTrue(SolidUtil.hasSupport(context, new Vec3i(4, 4, 1)));
    }

    @Test
    void worldFloorSupportsTheLowestLevel() {
        assertTrue(SolidUtil.hasSupport(context, new Vec3i(4, 4, 0)));
    }

    @Test
    void airBelowGivesNoSupport() {
        assertFalse(SolidUtil.hasSupport(context, new Vec3i(4, 4, 2)));
    }

    @Test
    void fullWaterBelowSupports() {
        context.fluid().setLevel(new Vec3i(4, 4, 1), FluidContext.MAX_LEVEL);
        assertTrue(SolidUtil.hasSupport(context, new Vec3i(4, 4, 2)));
    }

    @Test
    void partialWaterBelowDoesNotSupport() {
        context.fluid().setLevel(new Vec3i(4, 4, 1), 3);
        assertFalse(SolidUtil.hasSupport(context, new Vec3i(4, 4, 2)));
    }

    @Test
    void waterInTheCellItselfSupports() {
        context.fluid().setLevel(new Vec3i(4, 4, 2), 2);
        assertTrue(SolidUtil.hasSupport(context, new Vec3i(4, 4, 2)));
    }
}
