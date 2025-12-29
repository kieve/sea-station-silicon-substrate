package ca.kieve.ssss.event;

/**
 * Marker interface for system events.
 * System events are used for system-to-system communication within a tick cycle.
 * Events are created by one system and consumed by another, then cleared at the end of the tick.
 */
public interface SystemEvent {}
