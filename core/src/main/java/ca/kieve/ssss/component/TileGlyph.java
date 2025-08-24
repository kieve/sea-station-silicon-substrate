package ca.kieve.ssss.component;

import com.badlogic.gdx.graphics.g2d.BitmapFont;

public record TileGlyph(
    BitmapFont font,
    char glyph,
    float dx,
    float dy
) implements Component {
}
