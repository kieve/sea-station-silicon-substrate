package ca.kieve.ssss.component;

/**
 * Represents the physical size of an entity.
 * Used for passability checks through size-restricted passages like mouse holes.
 */
public enum Size implements Component {
    TINY,
    SMALL,
    MEDIUM,
    LARGE,
    GIGANTIC
}
