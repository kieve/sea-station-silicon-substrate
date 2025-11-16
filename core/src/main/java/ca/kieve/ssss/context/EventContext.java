package ca.kieve.ssss.context;

import ca.kieve.ssss.event.EventType;
import dev.dominion.ecs.api.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context for managing events generated during a game tick.
 * Events are created by systems (e.g., InteractSystem) and consumed by other systems.
 * Events should be cleared at the end of each tick cycle.
 */
public class EventContext {
    private final Map<EventType, List<Entity>> m_events = new HashMap<>();

    public void addEvent(EventType type, Entity entity) {
        m_events.computeIfAbsent(type, k -> new ArrayList<>()).add(entity);
    }

    public List<Entity> getEvents(EventType type) {
        var events = m_events.get(type);
        if (events == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(events);
    }

    public void clear() {
        m_events.clear();
    }
}
