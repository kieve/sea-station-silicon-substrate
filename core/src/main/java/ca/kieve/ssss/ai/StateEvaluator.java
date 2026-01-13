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
 * Utility class providing shared state evaluation logic.
 * Used by both AiControllerSystem and RandomBranchState.
 */
public final class StateEvaluator {
    private StateEvaluator() {}

    /**
     * Evaluates conditions for a state definition. Returns true if all pass.
     *
     * @param gameContext The game context
     * @param aiContext The AI controller context for condition resolution
     * @param entity The entity being evaluated
     * @param state The AI state instance
     * @param stateDef The state definition containing conditions
     * @return true if all conditions pass (or no conditions defined)
     */
    public static boolean evaluateConditions(
            GameContext gameContext,
            AiControllerContext aiContext,
            Entity entity,
            AiState state,
            StateDefinition stateDef
    ) {
        List<ConditionDefinition> conditions = stateDef.conditions();
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        int priority = stateDef.priority();
        ConditionContext condContext = new ConditionContext(
            gameContext, entity, state, priority);

        for (ConditionDefinition condDef : conditions) {
            Condition condition = aiContext.getCondition(condDef);
            if (!condition.evaluate(condContext)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Finds the first state whose conditions pass.
     * States should be pre-sorted by priority.
     *
     * @param gameContext The game context
     * @param aiContext The AI controller context
     * @param entity The entity being evaluated
     * @param controller The AI controller component
     * @param sortedStates States sorted by priority (lowest first)
     * @param factory The behavior factory for creating states
     * @return The first state definition whose conditions pass, or null if none match
     */
    public static StateDefinition selectState(
            GameContext gameContext,
            AiControllerContext aiContext,
            Entity entity,
            AiController controller,
            List<StateDefinition> sortedStates,
            BehaviorFactory factory
    ) {
        for (StateDefinition stateDef : sortedStates) {
            AiState state = getOrCreateState(controller, stateDef, factory);
            if (evaluateConditions(gameContext, aiContext, entity, state, stateDef)) {
                return stateDef;
            }
        }
        return null;
    }

    /**
     * Notifies all conditions of a state that it was selected.
     * Called after state selection to allow conditions to update their state.
     *
     * @param gameContext The game context
     * @param aiContext The AI controller context
     * @param entity The entity
     * @param state The AI state instance
     * @param stateDef The selected state definition
     */
    public static void notifyConditionsOfSelection(
            GameContext gameContext,
            AiControllerContext aiContext,
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
            gameContext, entity, state, priority);

        for (ConditionDefinition condDef : conditions) {
            Condition condition = aiContext.getCondition(condDef);
            condition.onStateSelected(condContext);
        }
    }

    /**
     * Gets or creates an AiState for a definition.
     *
     * @param controller The AI controller component
     * @param definition The state definition
     * @param factory The behavior factory for creating new states
     * @return The existing or newly created state
     */
    public static AiState getOrCreateState(
            AiController controller,
            StateDefinition definition,
            BehaviorFactory factory
    ) {
        String stateId = definition.state();
        AiState state = controller.getState(stateId);
        if (state == null) {
            state = factory.createState(definition);
            controller.setState(stateId, state);
        }
        return state;
    }
}
