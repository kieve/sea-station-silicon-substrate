package ca.kieve.ssss.ai.state;

import ca.kieve.ssss.ai.StateEvaluator;
import ca.kieve.ssss.ai.behavior.AiController;
import ca.kieve.ssss.ai.behavior.BehaviorFactory;
import ca.kieve.ssss.ai.behavior.BranchDefinition;
import ca.kieve.ssss.ai.behavior.StateDefinition;
import ca.kieve.ssss.ai.reset.ResetCondition;
import ca.kieve.ssss.ai.reset.ResetContext;
import ca.kieve.ssss.context.AiControllerContext;
import ca.kieve.ssss.context.GameContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.dominion.ecs.api.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Meta-state that contains nested state branches.
 * Randomly selects one branch and evaluates only its nested states.
 *
 * YAML properties:
 * - resetCondition: Name of reset condition class (e.g., "AttackerChange")
 * - branches: List of branch definitions with name and nested states
 */
public class RandomBranchState extends AiState {
    private static final String RESET_PACKAGE = "ca.kieve.ssss.ai.reset.";

    private int m_priority;
    private String m_resetConditionType;
    private List<BranchDefinition> m_branches;
    private final BehaviorFactory m_behaviorFactory = new BehaviorFactory();
    private final Map<String, Map<String, AiState>> m_nestedStates = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(Map<String, Object> properties) {
        super.initialize(properties);

        m_priority = ((Number) properties.get("_priority")).intValue();
        m_resetConditionType = (String) properties.get("resetCondition");

        // Parse branches from YAML using Jackson
        List<Map<String, Object>> branchMaps =
            (List<Map<String, Object>>) properties.get("branches");
        m_branches = parseBranches(branchMaps);

        // Pre-create nested AiState instances for each branch
        for (BranchDefinition branch : m_branches) {
            Map<String, AiState> branchStates = new HashMap<>();
            for (StateDefinition stateDef : branch.states()) {
                AiState state = m_behaviorFactory.createState(stateDef);
                branchStates.put(stateDef.state(), state);
            }
            m_nestedStates.put(branch.name(), branchStates);
        }
    }

    @Override
    public void execute(StateContext context) {
        AiController controller = context.controller();
        Entity entity = context.entity();
        GameContext gameContext = context.gameContext();

        // Get or create per-entity branch data
        RandomBranchData data = controller.getBranchData(m_priority);
        if (data == null) {
            data = new RandomBranchData();
            data.setResetCondition(createResetCondition());
            controller.setBranchData(m_priority, data);
        }

        ResetContext resetContext = new ResetContext(gameContext, entity);

        // Check if we need to reset the branch selection
        ResetCondition resetCondition = data.getResetCondition();
        if (data.getSelectedBranch() == null ||
            (resetCondition != null && resetCondition.shouldReset(resetContext))) {
            // Make a new random selection
            selectRandomBranch(data, gameContext.random());
            if (resetCondition != null) {
                resetCondition.recordState(resetContext);
            }
        }

        String selectedBranchName = data.getSelectedBranch();
        BranchDefinition selectedBranch = findBranch(selectedBranchName);
        if (selectedBranch == null) {
            return;
        }

        // Evaluate nested states in the selected branch
        executeNestedStates(context, selectedBranch);
    }

    private void executeNestedStates(StateContext parentContext, BranchDefinition branch) {
        GameContext gameContext = parentContext.gameContext();
        AiControllerContext aiContext = gameContext.aiController();
        Entity entity = parentContext.entity();
        AiController controller = parentContext.controller();

        Map<String, AiState> branchStates = m_nestedStates.get(branch.name());
        if (branchStates == null || branchStates.isEmpty()) {
            return;
        }

        // Sort nested states by priority
        List<StateDefinition> sortedStates = branch.states().stream()
            .sorted(Comparator.comparingInt(StateDefinition::priority))
            .toList();

        // Find first state whose conditions pass
        StateDefinition selectedState = null;
        AiState selectedAiState = null;
        for (StateDefinition stateDef : sortedStates) {
            AiState state = branchStates.get(stateDef.state());
            if (state != null &&
                StateEvaluator.evaluateConditions(
                    gameContext, aiContext, entity, state, stateDef)) {
                selectedState = stateDef;
                selectedAiState = state;
                break;
            }
        }

        if (selectedState == null || selectedAiState == null) {
            return;
        }

        // Notify conditions and execute
        StateEvaluator.notifyConditionsOfSelection(
            gameContext, aiContext, entity, selectedAiState, selectedState);

        // Resolve target for the nested state
        Entity target = aiContext.resolveTarget(
            entity, selectedState.target(), gameContext.ecs());
        StateContext nestedContext = new StateContext(
            gameContext, entity, controller, target);

        selectedAiState.execute(nestedContext);
    }

    private void selectRandomBranch(RandomBranchData data, Random random) {
        if (m_branches.isEmpty()) {
            return;
        }
        int idx = random.nextInt(m_branches.size());
        data.setSelectedBranch(m_branches.get(idx).name());
    }

    private BranchDefinition findBranch(String name) {
        return m_branches.stream()
            .filter(b -> b.name().equals(name))
            .findFirst()
            .orElse(null);
    }

    private ResetCondition createResetCondition() {
        if (m_resetConditionType == null || m_resetConditionType.isEmpty()) {
            return null;
        }
        try {
            String className = m_resetConditionType;
            if (!className.endsWith("Reset")) {
                className += "Reset";
            }
            Class<?> resetClass = Class.forName(RESET_PACKAGE + className);
            ResetCondition condition = (ResetCondition) resetClass
                .getDeclaredConstructor().newInstance();
            condition.initialize(m_properties);
            return condition;
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create reset condition: " + m_resetConditionType, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<BranchDefinition> parseBranches(List<Map<String, Object>> branchMaps) {
        if (branchMaps == null) {
            return List.of();
        }

        List<BranchDefinition> branches = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        for (Map<String, Object> branchMap : branchMaps) {
            String name = (String) branchMap.get("name");
            List<Map<String, Object>> stateMaps =
                (List<Map<String, Object>>) branchMap.get("states");

            List<StateDefinition> states = new ArrayList<>();
            if (stateMaps != null) {
                for (Map<String, Object> stateMap : stateMaps) {
                    StateDefinition stateDef = mapper.convertValue(
                        stateMap, StateDefinition.class);
                    states.add(stateDef);
                }
            }

            branches.add(new BranchDefinition(name, states));
        }

        return branches;
    }
}
