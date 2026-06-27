package ca.kieve.ssss.system;

import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.context.GameContext;

import static ca.kieve.ssss.util.TickStage.AWAIT_INPUT;
import static ca.kieve.ssss.util.TickStage.POST_TICK;
import static ca.kieve.ssss.util.TickStage.PRE_TICK;
import static ca.kieve.ssss.util.TickStage.REPORT_RESULTS;
import static ca.kieve.ssss.util.TickStage.TICK;
import static ca.kieve.ssss.util.TurnPhase.AI;
import static ca.kieve.ssss.util.TurnPhase.PLAYER;

/*
 * The general formula for speed is as follows:
 *
 * 100 * 100 / speed
 * 100 * 100 / 50 = 200 ticks to act
 * 100 * 100 / 200  = 50 ticks to act
 *
 * Systems tick in 4 stages:
 * AWAIT_INPUT -> Game waits for player input. Rendering only occurs here.
 * PRE_TICK -> Entities signal intent (e.g., "I want to move left")
 * TICK -> Processing based on preTick events (e.g., collision, mining, movement)
 * POST_TICK -> State finalization (e.g., velocity reset to zero)
 *
 * Turns are processed in two phases:
 * PLAYER phase -> Player completes a full turn (PRE_TICK -> TICK -> POST_TICK)
 *      AI entities cannot act during this phase.
 * AI phase -> AI entities catch up to current time (PRE_TICK -> TICK -> POST_TICK loops)
 *      Player cannot act during this phase.
 *
 * After AI phase completes, returns to AWAIT_INPUT for the next player turn.
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
        m_gameContext.perf().report();
        m_clock.setTurnPhase(PLAYER);
        m_clock.setTickStage(PRE_TICK);
        m_clock.setUserInputRegistered(false);

        // Set so the player can act.
        m_gameContext.ecs().findEntitiesWith(
            PlayerController.class,
            Speed.class
        ).forEach(with2 -> with2.comp2().canAct = true);
    }

    @Override
    public void preTick() {
        m_clock.setTickStage(TICK);

        // During player's turn, player's canAct was already set in awaitingUserInput.
        if (m_clock.isPlayerTurn()) {
            return;
        }

        updateCanAct();
    }

    private void updateCanAct() {
        var currentTime = m_clock.getCurrentTime();
        var withSpeeds = m_gameContext.ecs().findEntitiesWith(Speed.class);
        for (var with : withSpeeds) {
            if (with.entity().has(PlayerController.class)) {
                continue;
            }

            var speed = with.comp();
            if (speed.canActAt > currentTime) {
                speed.canAct = false;
                continue;
            }
            speed.canAct = true;
            var ticksToAct = getTicksToAct(speed.val);
            speed.canActAt = currentTime + ticksToAct;
        }
    }

    @Override
    public void tick() {
        m_clock.setTickStage(POST_TICK);
    }

    @Override
    public void postTick() {
        if (m_clock.isPlayerTurn()) {
            playerPostTick();
        } else {
            aiPostTick();
        }
    }

    private void playerPostTick() {
        m_gameContext.ecs().findEntitiesWith(
            PlayerController.class,
            Speed.class
        ).forEach(with2 -> with2.comp2().canAct = false);

        m_clock.setTurnPhase(AI);
        m_clock.setTickStage(PRE_TICK);
    }

    private void aiPostTick() {
        var minNextAct = m_clock.getTargetTime();
        var withSpeeds = m_gameContext.ecs().findEntitiesWith(Speed.class);
        for (var with : withSpeeds) {
            if (with.entity().has(PlayerController.class)) {
                continue;
            }
            minNextAct = Math.min(minNextAct, with.comp().canActAt);
        }

        m_clock.setCurrentTime(minNextAct);
        if (minNextAct < m_clock.getTargetTime()) {
            // More AI needs to act
            m_clock.setTickStage(PRE_TICK);
        } else {
            // All AI have acted, report accumulated results before awaiting input
            m_clock.setTurnPhase(PLAYER);
            m_clock.setTickStage(REPORT_RESULTS);
        }
    }

    @Override
    public void reportResults() {
        m_clock.setTickStage(AWAIT_INPUT);
        m_gameContext.render().markDirty();
    }

    @Override
    public void run() {
        super.run();
    }
}
