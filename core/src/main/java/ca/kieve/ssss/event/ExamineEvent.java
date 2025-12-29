package ca.kieve.ssss.event;

import dev.dominion.ecs.api.Entity;

/**
 * Event data for EXAMINE events.
 * Created by InteractSystem when player bumps into an examinable entity.
 * Consumed by ExamineSystem.
 */
public record ExamineEvent(Entity target) implements Event {}
