package ca.kieve.ssss.ai.behavior;

import ca.kieve.ssss.ai.state.AiState;
import ca.kieve.ssss.component.Component;
import ca.kieve.ssss.util.Vec3i;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime state for AI behavior. Holds instantiated states and state-specific data.
 */
public class AiController implements Component {
    private final Map<String, AiState> m_states = new HashMap<>();
    private String m_currentStateId;

    // State-specific persistent data
    private Vec3i m_wanderInitialPos;
    private boolean m_wanderGoingUp = true;

    public AiState getState(String stateId) {
        return m_states.get(stateId);
    }

    public void setState(String stateId, AiState state) {
        m_states.put(stateId, state);
    }

    public String getCurrentStateId() {
        return m_currentStateId;
    }

    public void setCurrentStateId(String stateId) {
        m_currentStateId = stateId;
    }

    // WanderState data accessors
    public Vec3i getWanderInitialPos() {
        return m_wanderInitialPos;
    }

    public void setWanderInitialPos(Vec3i pos) {
        m_wanderInitialPos = pos.copy();
    }

    public boolean isWanderGoingUp() {
        return m_wanderGoingUp;
    }

    public void setWanderGoingUp(boolean goingUp) {
        m_wanderGoingUp = goingUp;
    }
}
