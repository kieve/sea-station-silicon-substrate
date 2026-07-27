package ca.kieve.ssss.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.context.FluidContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterExamineTest {
    private final Vec3i pos = new Vec3i(1, 1, 0);

    private FluidContext fluid;

    @BeforeEach
    void setUp() {
        fluid = new FluidContext();
        fluid.init(new Vec3i(4, 4, 2));
    }

    @Test
    void nameReflectsDepthBand() {
        fluid.setLevel(pos, 1);
        assertEquals("Shallow water", WaterExamine.name(fluid, pos));

        fluid.setLevel(pos, 2);
        assertEquals("Water", WaterExamine.name(fluid, pos));

        fluid.setLevel(pos, 3);
        assertEquals("Deep water", WaterExamine.name(fluid, pos));

        fluid.setLevel(pos, FluidContext.MAX_LEVEL);
        assertEquals("Deep water", WaterExamine.name(fluid, pos));
    }

    @Test
    void descriptionVariesByDepthAndMentionsSeawater() {
        fluid.setLevel(pos, 1);
        var shallow = WaterExamine.description(fluid, pos);

        fluid.setLevel(pos, FluidContext.MAX_LEVEL);
        var full = WaterExamine.description(fluid, pos);

        assertTrue(shallow.contains("Cold seawater"));
        assertTrue(full.contains("Cold seawater"));
        assertTrue(!shallow.equals(full));
    }
}
