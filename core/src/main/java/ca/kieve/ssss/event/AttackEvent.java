package ca.kieve.ssss.event;

import dev.dominion.ecs.api.Entity;

/**
 * Event for attack actions between entities.
 * Created by InteractSystem (player attacks) or AI systems (AI attacks).
 * Consumed by AttackSystem.
 */
public record AttackEvent(Entity attacker, Entity target) implements SystemEvent {}
