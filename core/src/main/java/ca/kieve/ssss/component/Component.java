package ca.kieve.ssss.component;

import ca.kieve.ssss.context.GameContext;

import dev.dominion.ecs.api.Entity;

public interface Component {
    default void cleanup(GameContext context, Entity entity) {}
}
