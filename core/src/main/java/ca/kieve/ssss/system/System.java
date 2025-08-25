package ca.kieve.ssss.system;

import ca.kieve.ssss.context.ClockContext;
import ca.kieve.ssss.context.GameContext;

public abstract class System implements Runnable {
    protected final GameContext m_gameContext;
    protected final ClockContext m_clock;

    public System(GameContext gameContext) {
        m_gameContext = gameContext;
        m_clock = gameContext.clock();
    }

    public void awaitingUserInput() {}
    public void preTick() {}
    public void tick() {}
    public void postTick() {}

    @Override
    public void run() {
        switch (m_clock.getTickStage()) {
        case AWAIT_INPUT -> awaitingUserInput();
        case PRE_TICK -> preTick();
        case TICK -> tick();
        case POST_TICK -> postTick();
        }
    }
}
