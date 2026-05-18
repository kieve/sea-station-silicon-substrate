package ca.kieve.ssss.component;

import ca.kieve.ssss.content.map.ConnectorDirection;

/**
 * Named anchor point inside a map. Lives on a connector entity in the
 * map's {@code connectors:} section together with a {@link Position}
 * component; the entity's id is the connector's instance name.
 *
 * <p>{@link #direction} is required when the connector is used as the
 * {@code localConnector} side of a submap docking pair (so the loader
 * knows which side the child lands on). Pure remote-only connectors
 * may leave it {@code null}.
 *
 * <p>This is an {@link EngineComponent} — load-time only, never
 * attached to a live ECS entity.
 */
public class Connector implements EngineComponent {
    public ConnectorDirection direction;

    public Connector() {
    }

    public Connector(ConnectorDirection direction) {
        this.direction = direction;
    }
}
