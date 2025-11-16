package ca.kieve.ssss.component;

import com.badlogic.gdx.graphics.Color;

/**
 * Marks an entity to be rendered as a debug rectangle outline.
 * Useful for visualizing entities that don't have a visible sprite.
 */
public record DebugRect(Color color) implements Component {
    public DebugRect() {
        this(Color.MAGENTA);
    }
}
