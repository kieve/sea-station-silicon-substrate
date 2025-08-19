package ca.kieve.ssss.component;

import dev.dominion.ecs.api.Entity;

import ca.kieve.ssss.context.GameContext;

public interface Component {
    default void cleanup(GameContext context, Entity entity) {}
}
