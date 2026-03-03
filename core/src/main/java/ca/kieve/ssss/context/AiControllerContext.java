package ca.kieve.ssss.context;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.ai.behavior.ConditionDefinition;
import ca.kieve.ssss.ai.behavior.TargetType;
import ca.kieve.ssss.ai.condition.Condition;
import ca.kieve.ssss.component.LastAttacker;
import ca.kieve.ssss.component.PlayerController;
import ca.kieve.ssss.content.ReflectionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Context for AI controller state. Caches conditions since they are defined at
 * compile time and don't need to be recreated each tick.
 */
public class AiControllerContext {
    private final Map<ConditionDefinition, Condition> m_conditionCache = new HashMap<>();

    public Condition getCondition(ConditionDefinition definition) {
        return m_conditionCache.computeIfAbsent(definition, this::createCondition);
    }

    private Condition createCondition(ConditionDefinition definition) {
        return ReflectionFactory.createCondition(definition.type(), definition.properties());
    }

    /**
     * Resolves a target entity based on the target type.
     * Centralized to avoid duplication between conditions and state systems.
     */
    public Entity resolveTarget(Entity entity, TargetType targetType, Dominion ecs) {
        return switch (targetType) {
        case PLAYER -> {
            var results = ecs.findEntitiesWith(PlayerController.class);
            var it = results.iterator();
            yield it.hasNext() ? it.next().entity() : null;
        }
        case LAST_ATTACKER -> {
            var lastAttacker = entity.get(LastAttacker.class);
            yield lastAttacker != null ? lastAttacker.attacker : null;
        }
        };
    }
}
