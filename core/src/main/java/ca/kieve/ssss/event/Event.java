package ca.kieve.ssss.event;

/**
 * Marker interface for event data.
 * Events are used for system-to-system communication within a tick cycle.
 * Events are created by one system and consumed by another, then cleared at the end of the tick.
 */
public interface Event {}
