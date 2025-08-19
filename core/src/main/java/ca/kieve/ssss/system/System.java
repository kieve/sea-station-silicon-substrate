package ca.kieve.ssss.system;

import ca.kieve.ssss.context.GameContext;

public abstract class System implements Runnable {
    protected final GameContext m_gameContext;

    public System(GameContext gameContext) {
        m_gameContext = gameContext;
    }
}
