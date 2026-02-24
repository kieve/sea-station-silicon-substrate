package ca.kieve.ssss.component;

import ca.kieve.ssss.annotations.EditorIgnore;
import ca.kieve.ssss.annotations.EditorRef;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public record TileGlyph(
    @EditorRef(
            value = "glyphId",
            source = EditorRef.Source.GLYPH)
    BitmapFont font,
    @EditorIgnore char glyph,
    @EditorIgnore float dx,
    @EditorIgnore float dy
) implements Component {
}
