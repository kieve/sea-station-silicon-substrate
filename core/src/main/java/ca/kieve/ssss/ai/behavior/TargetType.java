package ca.kieve.ssss.ai.behavior;

/**
 * Special target types for AI conditions.
 */
public enum TargetType {
    PLAYER,        // Resolves to entity with PlayerController component
    LAST_ATTACKER  // Resolves to entity from LastAttacker component
}
