package ca.kieve.ssss.ai.condition;

import ca.kieve.ssss.ai.condition.data.MaxTimesConditionData;

import java.util.Map;

/**
 * Condition that is true only up to a maximum number of times.
 * Property: maxTimes (int) - maximum number of times this can return true.
 */
public class MaxTimesCondition implements Condition {
    private int m_maxTimes;

    @Override
    public void initialize(Map<String, Object> properties) {
        m_maxTimes = ((Number) properties.get("maxTimes")).intValue();
    }

    @Override
    public boolean evaluate(ConditionContext context) {
        MaxTimesConditionData data = getOrCreateData(context);
        return data.getTimesTriggered() < m_maxTimes;
    }

    @Override
    public void onStateSelected(ConditionContext context) {
        MaxTimesConditionData data = getOrCreateData(context);
        data.incrementTimes();
    }

    private MaxTimesConditionData getOrCreateData(ConditionContext context) {
        int priority = context.statePriority();
        MaxTimesConditionData data = (MaxTimesConditionData)
            context.state().getConditionData(priority);
        if (data == null) {
            data = new MaxTimesConditionData();
            context.state().setConditionData(priority, data);
        }
        return data;
    }
}
