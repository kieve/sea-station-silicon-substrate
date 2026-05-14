package ca.kieve.ssss.context;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

import java.util.List;

public class GhostTile {
    public final char glyph;
    public final BitmapFont font;
    public final float dx;
    public final float dy;
    public final Color color;
    public final List<GhostEntity> entities;

    public GhostTile(
        char glyph,
        BitmapFont font,
        float dx,
        float dy,
        Color color,
        List<GhostEntity> entities
    ) {
        this.glyph = glyph;
        this.font = font;
        this.dx = dx;
        this.dy = dy;
        this.color = new Color(color);
        this.entities = List.copyOf(entities);
    }
}
