package ca.kieve.ssss.context;

import ca.kieve.ssss.component.TileGlyph;

/**
 * Holds components that have been removed from the player entity temporarily.
 * Used to store player-specific data when the player is socketed into a body.
 */
public class PlayerContext {
    /**
     * The player's TileGlyph, stored when socketed and restored when ejected.
     */
    public TileGlyph tileGlyph;

    /**
     * The player's original speed value, stored when socketed and restored when ejected.
     * -1 indicates no speed is cached.
     */
    public int originalSpeed = -1;
}
