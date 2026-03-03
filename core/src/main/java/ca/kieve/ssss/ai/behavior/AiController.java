package ca.kieve.ssss.ai.behavior;

import ca.kieve.ssss.ai.state.AiState;
import ca.kieve.ssss.ai.state.RandomBranchData;
import ca.kieve.ssss.annotations.EditorIgnore;
import ca.kieve.ssss.annotations.EditorRef;
import ca.kieve.ssss.component.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime state for AI behavior. Holds instantiated states.
 */
public class AiController implements Component {
    @EditorRef(value = "behavior", source = EditorRef.Source.BEHAVIOR)
    private final List<StateDefinition> m_stateDefinitions;
    @EditorIgnore
    private final Map<String, AiState> m_states = new HashMap<>();
    @EditorIgnore
    private final Map<Integer, RandomBranchData> m_branchData = new HashMap<>();
    @EditorIgnore
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

    public RandomBranchData getBranchData(int priority) {
        return m_branchData.get(priority);
    }

    public void setBranchData(int priority, RandomBranchData data) {
        m_branchData.put(priority, data);
    }
}
