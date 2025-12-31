package ca.kieve.ssss.ai.condition.data;

/**
 * Tracks how many times a MaxTimes condition has been triggered.
 */
public class MaxTimesConditionData implements ConditionData {
    private int m_timesTriggered;

    public int getTimesTriggered() {
        return m_timesTriggered;
    }

    public void incrementTimes() {
        m_timesTriggered++;
    }
}
