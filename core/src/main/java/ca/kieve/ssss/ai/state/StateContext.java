package ca.kieve.ssss.ai.state;

import ca.kieve.ssss.ai.behavior.AiController;
import ca.kieve.ssss.context.GameContext;

import dev.dominion.ecs.api.Entity;

/**
 * Context provided to states during execution.
 */
public record StateContext(
    GameContext gameContext,
    Entity entity,
    AiController controller,
    Entity targetEntity  // Resolved target (e.g., player)
) {}
