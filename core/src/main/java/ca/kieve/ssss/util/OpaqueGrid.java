package ca.kieve.ssss.util;

/**
 * Predicate for whether a 2D tile blocks light/vision.
 * Used by FOV calculation today; reusable for any future light, laser, or
 * line-of-sight check that needs the same notion of "this tile is opaque".
 */
@FunctionalInterface
public interface OpaqueGrid {
    boolean isOpaque(int x, int y);
}
