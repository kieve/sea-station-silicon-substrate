package ca.kieve.ssss.ai.reset;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.context.GameContext;

/**
 * Context provided to reset conditions for evaluation.
 */
public record ResetContext(GameContext gameContext, Entity entity) {
}
