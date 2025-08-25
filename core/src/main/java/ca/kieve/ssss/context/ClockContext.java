package ca.kieve.ssss.context;

import ca.kieve.ssss.util.TickStage;

import static ca.kieve.ssss.util.TickStage.AWAIT_INPUT;

public class ClockContext {
    private long m_currentTime = 0;
    private TickStage m_tickStage = AWAIT_INPUT;
    private long m_targetTime = 0;
    private boolean m_userInputRegistered = false;

    public long getCurrentTime() {
        return m_currentTime;
    }

    public void setCurrentTime(long currentTime) {
        m_currentTime = currentTime;
    }


    public TickStage getTickStage() {
        return m_tickStage;
    }

    public void setTickStage(TickStage tickStage) {
        m_tickStage = tickStage;
    }

    public long getTargetTime() {
        return m_targetTime;
    }

    public void setTargetTime(long targetTime) {
        m_targetTime = targetTime;
    }

    public boolean isUserInputRegistered() {
        return m_userInputRegistered;
    }

    public void setUserInputRegistered(boolean userInputRegistered) {
        m_userInputRegistered = userInputRegistered;
    }
}
