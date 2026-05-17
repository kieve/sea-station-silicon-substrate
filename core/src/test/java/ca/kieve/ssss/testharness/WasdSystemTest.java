package ca.kieve.ssss.testharness;

import org.junit.jupiter.api.Test;

import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.util.Vec3i;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Coverage for {@link ca.kieve.ssss.system.WasdSystem}: the player-input
 * driver that converts directional input actions into one-tile velocity. The
 * actual movement and collision resolution happens in {@code VelocitySystem},
 * but the user-visible behaviour (press key → player moves / doesn't move)
 * spans both systems, so the tests live here.
 */
class WasdSystemTest {
    @Test
    void up_movesPlayerNorth() {
        assertSingleStep(InputAction.UP, 0, 1);
    }

    @Test
    void down_movesPlayerSouth() {
        assertSingleStep(InputAction.DOWN, 0, -1);
    }

    @Test
    void right_movesPlayerEast() {
        assertSingleStep(InputAction.RIGHT, 1, 0);
    }

    @Test
    void left_movesPlayerWest() {
        assertSingleStep(InputAction.LEFT, -1, 0);
    }

    @Test
    void upAndRight_inSameTurn_movesPlayerDiagonally() {
        TestEngine engine = TestEngine.createEmpty();
        Vec3i start = engine.controlledPosition();

        engine.pressAction(InputAction.UP);
        engine.pressAction(InputAction.RIGHT);
        engine.tickTurn();

        Vec3i end = engine.controlledPosition();
        assertEquals(start.x + 1, end.x);
        assertEquals(start.y + 1, end.y);
        assertEquals(start.z, end.z);
    }

    @Test
    void pressingIntoWall_doesNotMovePlayer() {
        TestEngine engine = TestEngine.create("test/wasd_walled_in.yaml");
        Vec3i start = engine.controlledPosition();
        long startTime = engine.clock().getCurrentTime();

        engine.pressAction(InputAction.UP);
        engine.tickTurn();

        Vec3i end = engine.controlledPosition();
        assertEquals(start, end, "wall should block movement");
        // The turn still resolves — pressing a direction is an action, even
        // when the move fails.
        assertEquals(
            true,
            engine.clock().getCurrentTime() > startTime,
            "clock should still advance when input is blocked"
        );
    }

    private static void assertSingleStep(InputAction action, int dx, int dy) {
        TestEngine engine = TestEngine.createEmpty();
        Vec3i start = engine.controlledPosition();

        engine.pressAction(action);
        engine.tickTurn();

        Vec3i end = engine.controlledPosition();
        assertEquals(start.x + dx, end.x, "x for action " + action);
        assertEquals(start.y + dy, end.y, "y for action " + action);
        assertEquals(start.z, end.z, "z for action " + action);
    }
}
