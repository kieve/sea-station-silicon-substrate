package ca.kieve.ssss.testharness;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.context.FluidContext;
import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FloorGatingTest {
    @Test
    void cannotStepIntoACellWithNoFloorBelow() {
        TestEngine engine = TestEngine.create("test/pit.yaml");
        Vec3i start = engine.controlledPosition();

        engine.pressAction(InputAction.UP);
        engine.tickTurn();

        assertEquals(start, engine.controlledPosition(), "stepping over a hole is blocked");
    }

    @Test
    void canStepOntoASupportedCell() {
        TestEngine engine = TestEngine.create("test/pit.yaml");
        Vec3i start = engine.controlledPosition();

        engine.pressAction(InputAction.RIGHT);
        engine.tickTurn();

        assertEquals(
            new Vec3i(start.x + 1, start.y, start.z),
            engine.controlledPosition(),
            "stepping onto solid floor is allowed"
        );
    }

    @Test
    void waterFillingThePitRestoresFooting() {
        TestEngine engine = TestEngine.create("test/pit.yaml");
        engine.context().fluid().setLevel(new Vec3i(2, 2, 0), FluidContext.MAX_LEVEL);

        engine.pressAction(InputAction.UP);
        engine.tickTurn();

        assertEquals(
            new Vec3i(2, 2, 1),
            engine.controlledPosition(),
            "full water below provides footing"
        );
    }
}
