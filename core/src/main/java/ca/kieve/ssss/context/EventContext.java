package ca.kieve.ssss.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.kieve.ssss.event.Event;

/**
 * Context for managing events generated during a game tick.
 * Events are created by systems (e.g., InteractSystem) and consumed by other systems.
 * Events should be cleared at the end of each tick cycle.
 */
public class EventContext {
    private final Map<Class<? extends Event>, List<Event>> m_events = new HashMap<>();

    public void addEvent(Event event) {
        m_events.computeIfAbsent(event.getClass(), _ -> new ArrayList<>()).add(event);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> List<T> getEvents(Class<T> eventClass) {
        var events = m_events.get(eventClass);
        if (events == null) {
            return Collections.emptyList();
        }
        return (List<T>) Collections.unmodifiableList(events);
    }

    public void clear() {
        m_events.clear();
    }
}
