package ca.kieve.ssss.ai.state;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.ai.behavior.AiController;
import ca.kieve.ssss.context.GameContext;

/**
 * Context provided to states during execution.
 */
public record StateContext(
    GameContext gameContext,
    Entity entity,
    AiController controller,
    Entity targetEntity // Resolved target (e.g., player)
) {
}
