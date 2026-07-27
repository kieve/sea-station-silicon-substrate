package ca.kieve.ssss.system;

import dev.dominion.ecs.api.Dominion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.kieve.ssss.component.Descriptor;
import ca.kieve.ssss.context.ExamineContext;
import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.context.PositionContext;
import ca.kieve.ssss.system.ExamineSystem.ExamineItem.ItemType;
import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamineSystemTest {
    private final Vec3i crosshair = new Vec3i(2, 3, 1);

    private PositionContext pos;
    private FluidContext fluid;
    private ExamineContext examine;

    @BeforeEach
    void setUp() {
        pos = new PositionContext();
        fluid = new FluidContext();
        fluid.init(new Vec3i(8, 8, 4));
        examine = new ExamineContext();
        examine.enter(crosshair);
    }

    @Test
    void visibleItemsIncludesWaterAtCrosshair() {
        fluid.setLevel(crosshair, 2);

        var items = ExamineSystem.visibleItems(pos, fluid, examine);

        assertEquals(1, items.size());
        var water = items.getFirst();
        assertEquals(ItemType.MAIN, water.type());
        assertEquals("Water", water.name());
        assertTrue(water.description().contains("Cold seawater"));
    }

    @Test
    void visibleItemsListsEntityThenWater() {
        Dominion ecs = Dominion.create();
        var crab = ecs.createEntity(new Descriptor("Crab", "A small crustacean."));
        pos.add(crab, crosshair);
        fluid.setLevel(crosshair, 3);

        var items = ExamineSystem.visibleItems(pos, fluid, examine);

        assertEquals(2, items.size());
        assertEquals("Crab", items.get(0).name());
        assertEquals("Deep water", items.get(1).name());
    }

    @Test
    void visibleItemsHasNoWaterWhenDry() {
        var items = ExamineSystem.visibleItems(pos, fluid, examine);
        assertTrue(items.isEmpty());
    }
}
