package ca.kieve.ssss.testharness;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.context.ClockContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.context.LogContext;
import ca.kieve.ssss.input.InputAction;
import ca.kieve.ssss.input.KeyState;
import ca.kieve.ssss.util.PlayerUtil;
import ca.kieve.ssss.util.TickStage;
import ca.kieve.ssss.util.Vec3i;

import java.util.List;

/**
 * Drives a headless {@link GameContext} for tests.
 *
 * <p>Vocabulary:
 * <ul>
 * <li><b>sub-tick</b> — one pass through every update system at the current
 *     stage ({@code updateSystems.forEach(run)}).</li>
 * <li><b>step</b> — one bundled engine pulse: a sub-tick, then sub-ticks
 *     repeated until the clock returns to {@code AWAIT_INPUT}. Mirrors a
 *     single {@code GameWindow.update()} call.</li>
 * <li><b>turn</b> — enough steps for the clock's current time to advance.
 *     Usually 2 steps: one to capture input, one to resolve player + AI back
 *     to {@code AWAIT_INPUT}.</li>
 * </ul>
 */
public class TestEngine {
    private static final int MAX_STEPS_PER_TURN = 1000;
    private static final int MAX_SUB_TICKS_PER_STEP = 10_000;

    private final GameContext m_context;
    private final ClockContext m_clock;

    /**
     * Builds a harness on the default empty test map. Most tests should use
     * this — see {@link TestGameContext#createEmpty()} for the map shape.
     */
    public static TestEngine createEmpty() {
        return new TestEngine(TestGameContext.createEmpty());
    }

    /**
     * Builds a harness on a specific map fixture (path relative to
     * {@code content/maps/}). Use this when geometry is part of the test
     * subject; drop a purpose-built YAML under
     * {@code core/src/test/resources/content/maps/test/}.
     */
    public static TestEngine create(String mapFilename) {
        return new TestEngine(TestGameContext.create(mapFilename));
    }

    /**
     * Wraps a {@link GameContext} the caller has already built. Use this when
     * a test needs to mutate the context (spawn entities, alter components)
     * between construction and driving.
     */
    public TestEngine(GameContext context) {
        m_context = context;
        m_clock = context.clock();
    }

    /**
     * The underlying game context. Most tests only need {@link #pressAction},
     * {@link #tickTurn}, and the assertion helpers — reach for this when you
     * need direct ECS / context access.
     */
    public GameContext context() {
        return m_context;
    }

    /**
     * Injects an input action by setting its key-state event flag.
     * Bypasses {@code Gdx.input} entirely.
     */
    public void pressAction(InputAction action) {
        KeyState state = m_context.input().getKeyState(action);
        if (state == null) {
            throw new IllegalStateException("No key state for action: " + action);
        }
        state.event = true;
    }

    /**
     * Runs one sub-tick, then loops sub-ticks until the clock returns to
     * {@code AWAIT_INPUT}. Mirrors a single {@code GameWindow.update()} call.
     * The inner loop is capped to catch broken stage transitions that would
     * otherwise hang the test.
     */
    public void step() {
        m_context.updateSystems().forEach(Runnable::run);
        int subTicks = 0;
        while (m_clock.getTickStage() != TickStage.AWAIT_INPUT) {
            m_context.updateSystems().forEach(Runnable::run);
            subTicks++;
            if (subTicks > MAX_SUB_TICKS_PER_STEP) {
                throw new IllegalStateException(
                    "step: stage did not return to AWAIT_INPUT after "
                        + MAX_SUB_TICKS_PER_STEP + " sub-ticks"
                );
            }
        }
    }

    /**
     * Advances simulation until the clock's current time moves past its
     * starting value. Each {@link #step()} call always returns at
     * {@code AWAIT_INPUT} by construction, so the only failure mode here is
     * queued input that never advances the clock — wrong input mode, no
     * controlled entity, missing Speed component, etc.
     */
    public int tickTurn() {
        long startTime = m_clock.getCurrentTime();
        int steps = 0;
        while (m_clock.getCurrentTime() == startTime) {
            step();
            steps++;
            if (steps > MAX_STEPS_PER_TURN) {
                throw new IllegalStateException(
                    "tickTurn: clock did not advance after " + steps + " steps"
                );
            }
        }
        return steps;
    }

    // ---- assertion helpers ----

    public Entity controlledEntity() {
        return PlayerUtil.getControlledEntity(m_context.ecs());
    }

    public Vec3i controlledPosition() {
        return PlayerUtil.getControlledPosition(m_context.ecs());
    }

    public List<LogContext.LogEntry> log() {
        return m_context.log().getMessages();
    }

    public ClockContext clock() {
        return m_clock;
    }
}
