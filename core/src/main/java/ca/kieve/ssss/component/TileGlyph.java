package ca.kieve.ssss.component;

import com.badlogic.gdx.graphics.g2d.BitmapFont;

public record TileGlyph(
    char glyph,
    BitmapFont font,
    float dx,
    float dy
) implements Component {
}
