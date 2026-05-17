package ca.kieve.ssss.testharness;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.util.TickStage;
import ca.kieve.ssss.util.TurnPhase;
import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the test harness. Each test stands up the full engine
 * against the default empty map and exercises one slice of behaviour. They
 * double as the API spec for {@link TestEngine}.
 */
class TestEngineSmokeTest {
    @Test
    void wasdSystem_pressUp_movesPlayerNorth() {
        TestEngine engine = TestEngine.createEmpty();

        Vec3i start = engine.controlledPosition();
        assertNotNull(start, "controlled entity must have a Position");

        engine.pressAction(InputAction.UP);
        engine.tickTurn();

        Vec3i end = engine.controlledPosition();
        assertEquals(start.x, end.x);
        assertEquals(start.y + 1, end.y);
        assertEquals(start.z, end.z);
    }

    @Test
    void wasdSystem_multipleUpPresses_compoundOverTurns() {
        TestEngine engine = TestEngine.createEmpty();

        Vec3i start = engine.controlledPosition();

        for (int i = 0; i < 3; i++) {
            engine.pressAction(InputAction.UP);
            engine.tickTurn();
        }

        Vec3i end = engine.controlledPosition();
        assertEquals(start.y + 3, end.y);
    }

    @Test
    void clockSystem_returnsToAwaitInputAfterTurn() {
        TestEngine engine = TestEngine.createEmpty();

        assertEquals(TickStage.AWAIT_INPUT, engine.clock().getTickStage());
        assertEquals(TurnPhase.PLAYER, engine.clock().getTurnPhase());
        long startTime = engine.clock().getCurrentTime();

        engine.pressAction(InputAction.WAIT);
        engine.tickTurn();

        assertEquals(TickStage.AWAIT_INPUT, engine.clock().getTickStage());
        assertEquals(TurnPhase.PLAYER, engine.clock().getTurnPhase());
        assertTrue(
            engine.clock().getCurrentTime() > startTime,
            "clock should advance after a WAIT turn"
        );
    }
}
