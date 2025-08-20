package ca.kieve.ssss.component;

import ca.kieve.ssss.util.Vec3i;

public record Velocity(
    Vec3i instant
) implements Component {
    public Velocity() {
        this(new Vec3i(0, 0, 0));
    }
}
