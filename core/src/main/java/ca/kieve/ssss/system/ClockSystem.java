package ca.kieve.ssss.system;

import ca.kieve.ssss.context.GameContext;

import static ca.kieve.ssss.util.TickStage.AWAIT_INPUT;
import static ca.kieve.ssss.util.TickStage.POST_TICK;
import static ca.kieve.ssss.util.TickStage.PRE_TICK;
import static ca.kieve.ssss.util.TickStage.TICK;

/*
 * The general formula for speed is as follows:
 *
 * 100 * 100 / speed
 * 100 * 100 / 50 = 200 ticks to act
 * 100 * 100 / 200  = 50 ticks to act
 *
 * Further, Systems tick in 3 stages:
 * preTick -> Internal processing, external pre-processing that affects the world
 *      Example: I want to move left
 * tick -> Processing in response to preTick events and state
 *      Example: Attempting to move there results in mining the block. Or, if empty, you move there.
 * postTick -> Processing that changes state that the tick state would have depended on
 *      Example: There was a wall, or you moved. But your velocity is back to zero
 *
 * Time effectively pauses when waiting for the user's input.
 */
public class ClockSystem extends System {
    public ClockSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void awaitingUserInput() {
        if (!m_clock.isUserInputRegistered()) {
            return;
        }
        m_clock.setTickStage(PRE_TICK);
        m_clock.setUserInputRegistered(false);
    }

    @Override
    public void preTick() {
        m_clock.setTickStage(TICK);
        // TODO: Process entities that can act, move time forward by their speed
        //       Until then... just move time.
        m_clock.setCurrentTime(m_clock.getTargetTime());
    }

    @Override
    public void tick() {
        m_clock.setTickStage(POST_TICK);
    }

    @Override
    public void postTick() {
        // TODO: Don't move to AWAIT_INPUT if we haven't reached the target time.
        m_clock.setTickStage(AWAIT_INPUT);
    }

    @Override
    public void run() {
        super.run();
    }
}
