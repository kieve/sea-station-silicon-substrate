package ca.kieve.ssss.component;

/**
 * Reference to a nested map. Lives on a submap entity in the parent's
 * {@code submaps:} section; the entity's id is the region's instance
 * name (defaults to the ref path minus extension if omitted).
 *
 * <p>Placement mode is implicit and derived from the entity's components:
 * <ul>
 *   <li>If the entity has a {@link Position} component → <b>offset
 *       mode</b>; the position is the submap's offset in the parent's
 *       coordinate space. {@link #localConnector} and
 *       {@link #remoteConnector} must be {@code null}.</li>
 *   <li>If the entity has no {@link Position} component →
 *       <b>connector mode</b>; both {@link #localConnector} and
 *       {@link #remoteConnector} must be set, and the loader computes
 *       the offset so the child's remote connector lands one cell past
 *       the parent's local connector in its declared direction.</li>
 * </ul>
 *
 * <p>This is an {@link EngineComponent} — load-time only, never
 * attached to a live ECS entity.
 */
public class Submap implements EngineComponent {
    public String ref;
    public String localConnector;
    public String remoteConnector;
    public boolean allowOverlap;

    public Submap() {
    }

    public Submap(String ref) {
        this.ref = ref;
    }
}
