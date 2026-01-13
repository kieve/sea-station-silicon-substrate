package ca.kieve.ssss.ai;

import ca.kieve.ssss.ai.behavior.AiController;
import ca.kieve.ssss.ai.behavior.BehaviorFactory;
import ca.kieve.ssss.ai.behavior.ConditionDefinition;
import ca.kieve.ssss.ai.behavior.StateDefinition;
import ca.kieve.ssss.ai.condition.Condition;
import ca.kieve.ssss.ai.condition.ConditionContext;
import ca.kieve.ssss.ai.state.AiState;
import ca.kieve.ssss.context.AiControllerContext;
import ca.kieve.ssss.context.GameContext;
import dev.dominion.ecs.api.Entity;

import java.util.List;

/**
 * Provides shared state evaluation logic.
 * Used by both AiControllerSystem and RandomBranchState.
 */
public class StateEvaluator {
    private final GameContext m_gameContext;
    private final AiControllerContext m_aiContext;
    private final BehaviorFactory m_behaviorFactory;

    public StateEvaluator(GameContext gameContext) {
        m_gameContext = gameContext;
        m_aiContext = gameContext.aiController();
        m_behaviorFactory = new BehaviorFactory();
    }

    /**
     * Evaluates conditions for a state definition. Returns true if all pass.
     */
    public boolean evaluateConditions(Entity entity, AiState state, StateDefinition stateDef) {
        List<ConditionDefinition> conditions = stateDef.conditions();
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        int priority = stateDef.priority();
        ConditionContext condContext = new ConditionContext(
            m_gameContext, entity, state, priority);

        for (ConditionDefinition condDef : conditions) {
            Condition condition = m_aiContext.getCondition(condDef);
            if (!condition.evaluate(condContext)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Finds the first state whose conditions pass.
     * States should be pre-sorted by priority.
     */
    public StateDefinition selectState(
            Entity entity,
            AiController controller,
            List<StateDefinition> sortedStates
    ) {
        for (StateDefinition stateDef : sortedStates) {
            AiState state = getOrCreateState(controller, stateDef);
            if (evaluateConditions(entity, state, stateDef)) {
                return stateDef;
            }
        }
        return null;
    }

    /**
     * Notifies all conditions of a state that it was selected.
     * Called after state selection to allow conditions to update their state.
     */
    public void notifyConditionsOfSelection(
            Entity entity,
            AiState state,
            StateDefinition stateDef
    ) {
        List<ConditionDefinition> conditions = stateDef.conditions();
        if (conditions == null || conditions.isEmpty()) {
            return;
        }

        int priority = stateDef.priority();
        ConditionContext condContext = new ConditionContext(
            m_gameContext, entity, state, priority);

        for (ConditionDefinition condDef : conditions) {
            Condition condition = m_aiContext.getCondition(condDef);
            condition.onStateSelected(condContext);
        }
    }

    /**
     * Gets or creates an AiState for a definition.
     */
    public AiState getOrCreateState(AiController controller, StateDefinition definition) {
        String stateId = definition.state();
        AiState state = controller.getState(stateId);
        if (state == null) {
            state = m_behaviorFactory.createState(definition);
            controller.setState(stateId, state);
        }
        return state;
    }
}
