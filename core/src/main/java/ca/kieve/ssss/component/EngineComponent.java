package ca.kieve.ssss.component;

/**
 * Marker subinterface for components that exist only as load-time /
 * editor-shaped data and must never be attached to a live ECS entity.
 *
 * <p>Examples: {@link Connector}, {@link Submap}. These ride through the
 * YAML → {@code MapEntityDefinition} → loader pipeline so the editor
 * can render and edit them with the same UI as real entities, but at
 * runtime they live in their own {@code MapDefinition} sections and
 * {@link ca.kieve.ssss.content.EntityFactory} refuses to instantiate
 * any {@code EngineComponent} onto a Dominion entity.
 *
 * <p>The editor's "Add Override" picker (component dropdown) also
 * filters these out — users can't add an engine component to anything
 * through the regular flow; engine components are attached only at
 * creation time by the dedicated Add Connector / Add Submap dialogs.
 */
public interface EngineComponent extends Component {
}
