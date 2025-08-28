package ca.kieve.ssss.system;

import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.component.WasdController;
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

    public static int getTicksToAct(int speed) {
        return (100 * 100) / speed;
    }

    @Override
    public void awaitingUserInput() {
        if (!m_clock.isUserInputRegistered()) {
            return;
        }
        m_clock.setTickStage(PRE_TICK);
        m_clock.setUserInputRegistered(false);

        // Set so the player can act.
        m_gameContext.ecs().findEntitiesWith(
            WasdController.class,
            Speed.class
        ).forEach(with2 -> with2.comp2().canAct = true);
    }

    @Override
    public void preTick() {
        m_clock.setTickStage(TICK);

        var currentTime = m_clock.getCurrentTime();
        var withSpeeds = m_gameContext.ecs().findEntitiesWith(Speed.class);
        for (var with : withSpeeds) {
            var speed = with.comp();

            // Can any entities now?
            if (speed.canActAt <= currentTime) {
                speed.canAct = true;
                var ticksToAct = getTicksToAct(speed.val);
                speed.canActAt = currentTime + ticksToAct;
            } else {
                speed.canAct = false;
            }
        }
    }

    @Override
    public void tick() {
        m_clock.setTickStage(POST_TICK);
    }

    @Override
    public void postTick() {
        var minNextAct = m_clock.getTargetTime();

        var withSpeeds = m_gameContext.ecs().findEntitiesWith(Speed.class);
        for (var with : withSpeeds) {
            var speed = with.comp();

            // Special case for the player...
            if (with.entity().has(WasdController.class)) {
                speed.canAct = false;
                continue;
            }
            minNextAct = Math.min(minNextAct, speed.canActAt);
        }

        m_clock.setCurrentTime(minNextAct);
        if (minNextAct < m_clock.getTargetTime()) {
            // Let the non-player AI act
            m_clock.setTickStage(PRE_TICK);
        } else {
            // Non-players have acted. Nothing can act again before the player.
            // Wait for input.
            m_clock.setTickStage(AWAIT_INPUT);
        }
    }

    @Override
    public void run() {
        super.run();
    }
}
