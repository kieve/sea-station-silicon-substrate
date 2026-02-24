package ca.kieve.ssss.context;

import static ca.kieve.ssss.util.TickStage.AWAIT_INPUT;
import static ca.kieve.ssss.util.TurnPhase.PLAYER;

import ca.kieve.ssss.system.ClockSystem;
import ca.kieve.ssss.util.TickStage;
import ca.kieve.ssss.util.TurnPhase;

public class ClockContext {
    private long m_currentTime = 0;
    private TurnPhase m_turnPhase = PLAYER;
    private TickStage m_tickStage = AWAIT_INPUT;
    private long m_targetTime = 0;
    private boolean m_userInputRegistered = false;

    public long getCurrentTime() {
        return m_currentTime;
    }

    public void setCurrentTime(long currentTime) {
        m_currentTime = currentTime;
    }

    public TurnPhase getTurnPhase() {
        return m_turnPhase;
    }

    public void setTurnPhase(TurnPhase turnPhase) {
        m_turnPhase = turnPhase;
    }

    public boolean isPlayerTurn() {
        return m_turnPhase == PLAYER;
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

    /**
     * Called when the player has performed an action that should advance time.
     * Sets the user input as registered and calculates the next time the player can act.
     *
     * @param speedVal the speed value of the acting entity
     */
    public void processPlayerActed(int speedVal) {
        m_userInputRegistered = true;
        m_targetTime = m_currentTime + ClockSystem.getTicksToAct(speedVal);
    }
}
