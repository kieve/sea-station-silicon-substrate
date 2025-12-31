package ca.kieve.ssss.system.ai;

import ca.kieve.ssss.ai.behavior.AiController;
import ca.kieve.ssss.ai.behavior.BehaviorFactory;
import ca.kieve.ssss.ai.behavior.ConditionDefinition;
import ca.kieve.ssss.ai.behavior.StateDefinition;
import ca.kieve.ssss.ai.condition.Condition;
import ca.kieve.ssss.ai.condition.ConditionContext;
import ca.kieve.ssss.ai.state.AiState;
import ca.kieve.ssss.ai.state.StateContext;
import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.component.Position;
import ca.kieve.ssss.component.Speed;
import ca.kieve.ssss.context.AiControllerContext;
import ca.kieve.ssss.context.GameContext;
import ca.kieve.ssss.system.System;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import java.util.Comparator;
import java.util.List;

/**
 * AI system that evaluates state definitions and executes states.
 */
public class AiControllerSystem extends System {
    private final Dominion m_ecs;
    private final AiControllerContext m_aiControllerContext;
    private final BehaviorFactory m_behaviorFactory;

    public AiControllerSystem(GameContext gameContext) {
        super(gameContext);
        m_ecs = gameContext.ecs();
        m_aiControllerContext = gameContext.aiController();
        m_behaviorFactory = new BehaviorFactory();
    }

    @Override
    public void tick() {
        var entities = m_ecs.findEntitiesWith(
            AiController.class,
            Position.class,
            Speed.class
        );

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

        // Sort states by priority (lower = higher priority, checked first)
        List<StateDefinition> sortedStates = stateDefinitions.stream()
            .sorted(Comparator.comparingInt(StateDefinition::priority))
            .toList();

        // Find first state whose conditions all pass
        StateDefinition selectedState = null;
        for (StateDefinition stateDef : sortedStates) {
            if (evaluateConditions(entity, stateDef.conditions())) {
                selectedState = stateDef;
                break;
            }
        }

        if (selectedState == null) {
            return;
        }

        // Handle state transitions
        String newStateId = selectedState.state();
        String currentStateId = controller.getCurrentStateId();

        if (!newStateId.equals(currentStateId)) {
            // Exit old state
            if (currentStateId != null) {
                AiState oldState = controller.getState(currentStateId);
                if (oldState != null) {
                    oldState.onExit(createStateContext(entity, controller));
                }
            }

            // Enter new state
            AiState newState = getOrCreateState(controller, selectedState);
            newState.onEnter(createStateContext(entity, controller));
            controller.setCurrentStateId(newStateId);
        }

        // Execute current state
        AiState currentState = controller.getState(newStateId);
        if (currentState != null) {
            currentState.execute(createStateContext(entity, controller));
        }
    }

    private boolean evaluateConditions(
        Entity entity,
        List<ConditionDefinition> conditions
    ) {
        if (conditions == null || conditions.isEmpty()) {
            return true;  // No conditions = always true
        }

        ConditionContext condContext = new ConditionContext(m_gameContext, entity);

        for (ConditionDefinition condDef : conditions) {
            Condition condition = m_aiControllerContext.getCondition(condDef);
            if (!condition.evaluate(condContext)) {
                return false;  // AND logic: all must pass
            }
        }

        return true;
    }

    private AiState getOrCreateState(AiController controller, StateDefinition definition) {
        String stateId = definition.state();
        AiState state = controller.getState(stateId);
        if (state == null) {
            state = m_behaviorFactory.createState(definition);
            controller.setState(stateId, state);
        }
        return state;
    }

    private StateContext createStateContext(Entity entity, AiController controller) {
        Entity player = findPlayer();
        return new StateContext(m_gameContext, entity, controller, player);
    }

    private Entity findPlayer() {
        var results = m_ecs.findEntitiesWith(PlayerController.class);
        var it = results.iterator();
        if (it.hasNext()) {
            return it.next().entity();
        }
        return null;
    }
}
