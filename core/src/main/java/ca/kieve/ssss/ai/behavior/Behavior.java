package ca.kieve.ssss.ai.behavior;

import ca.kieve.ssss.component.Component;

/**
 * Component that links an entity to a behavior definition by ID.
 * Loaded from YAML with: type: Behavior, id: player_hunter
 */
public record Behavior(String behaviorId) implements Component {
}
