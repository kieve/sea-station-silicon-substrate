package ca.kieve.ssss.system.ai;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.ai.StateEvaluator;
import ca.kieve.ssss.ai.behavior.AiController;
import ca.kieve.ssss.ai.behavior.StateDefinition;
import ca.kieve.ssss.ai.state.AiState;
import ca.kieve.ssss.ai.state.StateContext;
import ca.kieve.ssss.component.Health;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.context.AiControllerContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.system.System;

import java.util.Comparator;
import java.util.List;

/**
 * AI system that evaluates state definitions and executes states.
 */
public class AiControllerSystem extends System {
    private final Dominion m_ecs;
    private final AiControllerContext m_aiControllerContext;
    private final StateEvaluator m_stateEvaluator;

    public AiControllerSystem(GameContext gameContext) {
        super(gameContext);
        m_ecs = gameContext.ecs();
        m_aiControllerContext = gameContext.aiController();
        m_stateEvaluator = new StateEvaluator(gameContext);
    }

    @Override
    public void tick() {
        var entities = m_ecs.findEntitiesWith(AiController.class, Position.class, Speed.class);

        entities.forEach(result -> {
            var controller = result.comp1();
            var speed = result.comp3();
            Entity entity = result.entity();

            if (!speed.canAct) {
                return;
            }

            processController(entity, controller);
        });
    }

    private void processController(Entity entity, AiController controller) {
        List<StateDefinition> stateDefinitions = controller.getStateDefinitions();
        if (stateDefinitions == null || stateDefinitions.isEmpty()) {
            return;
        }

        // Check if entity is dead
        Health health = entity.get(Health.class);
        boolean isDead = health != null && health.hp <= 0;

        // Sort states by priority (lower = higher priority, checked first)
        // Dead entities can only run states that have an IsDead condition
        List<StateDefinition> sortedStates = stateDefinitions.stream()
            .filter(s -> !isDead || s.hasIsDeadCondition())
            .sorted(Comparator.comparingInt(StateDefinition::priority))
            .toList();

        // Find first state whose conditions all pass
        StateDefinition selectedState = m_stateEvaluator.selectState(
            entity,
            controller,
            sortedStates
        );

        if (selectedState == null) {
            return;
        }

        // Notify conditions that this state was selected
        AiState selectedAiState = controller.getState(selectedState.state());
        m_stateEvaluator.notifyConditionsOfSelection(entity, selectedAiState, selectedState);

        // Handle state transitions
        String newStateId = selectedState.state();
        String currentStateId = controller.getCurrentStateId();

        StateContext stateContext = createStateContext(entity, controller, selectedState);

        if (!newStateId.equals(currentStateId)) {
            // Exit old state
            if (currentStateId != null) {
                AiState oldState = controller.getState(currentStateId);
                if (oldState != null) {
                    oldState.onExit(stateContext);
                }
            }

            // Enter new state
            AiState newState = controller.getState(newStateId);
            newState.onEnter(stateContext);
            controller.setCurrentStateId(newStateId);
        }

        // Execute current state
        AiState currentState = controller.getState(newStateId);
        if (currentState != null) {
            currentState.execute(stateContext);
        }
    }

    private StateContext createStateContext(
        Entity entity,
        AiController controller,
        StateDefinition stateDef
    ) {
        Entity target = m_aiControllerContext.resolveTarget(entity, stateDef.target(), m_ecs);
        return new StateContext(m_gameContext, entity, controller, target);
    }
}
