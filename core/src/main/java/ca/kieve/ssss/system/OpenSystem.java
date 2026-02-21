package ca.kieve.ssss.system;

import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.event.OpenEvent;
import ca.kieve.ssss.util.OpenableUtil;

public class OpenSystem extends System {
    public OpenSystem(GameContext gameContext) {
        super(gameContext);
    }

    @Override
    public void tick() {
        var openEvents = m_gameContext.events().getEvents(OpenEvent.class);
        for (var event : openEvents) {
            OpenableUtil.open(m_gameContext, event.target());
        }
    }
}
