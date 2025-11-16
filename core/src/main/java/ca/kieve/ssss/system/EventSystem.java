package ca.kieve.ssss.system;

import ca.kieve.ssss.context.GameContext;

/**
 * System that clears events at the end of each tick cycle.
 * Must be registered as the last update system so all other systems
 * can consume events before they are cleared.
 */
public class EventSystem extends System {
    public EventSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void postTick() {
        m_gameContext.events().clear();
    }
}
