package ca.kieve.ssss.event;

import dev.dominion.ecs.api.Entity;

public record OpenEvent(Entity opener, Entity target) implements Event {}
