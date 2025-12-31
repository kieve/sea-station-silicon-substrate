package ca.kieve.ssss.ai.behavior;

import ca.kieve.ssss.ai.state.AiState;
import ca.kieve.ssss.component.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime state for AI behavior. Holds instantiated states.
 */
public class AiController implements Component {
    private final List<StateDefinition> m_stateDefinitions;
    private final Map<String, AiState> m_states = new HashMap<>();
    private String m_currentStateId;

    public AiController() {
        m_stateDefinitions = Collections.emptyList();
    }

    public AiController(List<StateDefinition> stateDefinitions) {
        m_stateDefinitions = stateDefinitions;
    }

    public List<StateDefinition> getStateDefinitions() {
        return m_stateDefinitions;
    }

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
}
