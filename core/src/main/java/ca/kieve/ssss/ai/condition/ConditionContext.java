package ca.kieve.ssss.ai.condition;

import ca.kieve.ssss.context.GameContext;
import dev.dominion.ecs.api.Entity;

/**
 * Context provided to conditions during evaluation.
 */
public record ConditionContext(
    GameContext gameContext,
    Entity entity
) {}
