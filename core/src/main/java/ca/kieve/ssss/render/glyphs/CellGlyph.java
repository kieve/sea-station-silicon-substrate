package ca.kieve.ssss.render.glyphs;

import com.badlogic.gdx.graphics.Color;

import ca.kieve.ssss.component.TileGlyph;
import ca.kieve.ssss.util.Vec3i;

public record CellGlyph(Vec3i cell, TileGlyph glyph, Color color, int priority) {
}
