package ca.kieve.ssss.component;

import ca.kieve.ssss.util.Vec3i;

/**
 * Marker component for entities that need ScurryConfig initialized
 * after map spawning. Consumed by ScurryInitSystem.
 */
public record ScurryInit(Vec3i initialDirection) implements Component {
    public ScurryInit() {
        this(null);
    }
}
