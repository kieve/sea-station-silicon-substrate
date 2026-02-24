package ca.kieve.ssss.ai.state;

import java.util.HashMap;
import java.util.Map;

import ca.kieve.ssss.ai.condition.data.ConditionData;
import ca.kieve.ssss.content.YamlInitializable;

/**
 * Abstract base class for AI states.
 */
public abstract class AiState implements YamlInitializable {
    protected Map<String, Object> m_properties;
    private final Map<Integer, ConditionData> m_conditionData = new HashMap<>();

    @Override
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

    public ConditionData getConditionData(int priority) {
        return m_conditionData.get(priority);
    }

    public void setConditionData(int priority, ConditionData data) {
        m_conditionData.put(priority, data);
    }
}
