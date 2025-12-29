package ca.kieve.ssss.context;

import ca.kieve.ssss.event.EventType;
import dev.dominion.ecs.api.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context for managing events generated during a game tick.
 * Events are created by systems (e.g., InteractSystem) and consumed by other systems.
 * Events should be cleared at the end of each tick cycle.
 */
public class EventContext {
    private final Map<EventType, List<Entity>> m_events = new HashMap<>();
    private final Set<Object> m_systemEvents = new HashSet<>();

    public void addEvent(EventType type, Entity entity) {
        m_events.computeIfAbsent(type, _ -> new ArrayList<>()).add(entity);
    }

    public List<Entity> getEvents(EventType type) {
        var events = m_events.get(type);
        if (events == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(events);
    }

    public void addSystemEvent(Object event) {
        m_systemEvents.add(event);
    }

    public <T> List<T> getSystemEvents(Class<T> type) {
        var result = new ArrayList<T>();
        for (var event : m_systemEvents) {
            if (type.isInstance(event)) {
                result.add(type.cast(event));
            }
        }
        return result;
    }

    public void clear() {
        m_events.clear();
        m_systemEvents.clear();
    }
}
