package ca.kieve.ssss.component;

/**
 * Component for blocks/tiles that restricts passage based on entity size.
 * Entities with a Size at or below maxSize can pass through.
 * Entities larger than maxSize are blocked.
 *
 * Blocks with this component should NOT have the Solid component - passability
 * is determined dynamically based on the moving entity's size.
 */
public record MaxPassableSize(Size maxSize) implements Component {
}
