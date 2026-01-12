package ca.kieve.ssss.component;

import ca.kieve.ssss.util.Vec3i;

/**
 * Configuration component for the scurry AI behavior.
 * Specifies wall-following direction and optional initial movement direction.
 */
public record ScurryConfig(
    boolean clockwise,
    Vec3i initialDirection  // Can be null for random
) implements Component {}
