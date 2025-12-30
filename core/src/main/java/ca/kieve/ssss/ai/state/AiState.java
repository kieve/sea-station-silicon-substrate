package ca.kieve.ssss.ai.state;

import java.util.Map;

/**
 * Abstract base class for AI states.
 */
public abstract class AiState {
    protected Map<String, Object> m_properties;

    public void initialize(Map<String, Object> properties) {
        m_properties = properties;
    }

    /**
     * Called when this state becomes active for an entity.
     */
    public void onEnter(StateContext context) {}

    /**
     * Called when this state becomes inactive for an entity.
     */
    public void onExit(StateContext context) {}

    /**
     * Execute this state's behavior.
     */
    public abstract void execute(StateContext context);
}
