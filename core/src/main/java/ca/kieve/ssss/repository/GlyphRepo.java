package ca.kieve.ssss.repository;

import com.badlogic.gdx.graphics.g2d.BitmapFont;

import ca.kieve.ssss.component.TileGlyph;

import static ca.kieve.ssss.repository.FontRepo.UBUNTU_32;
import static ca.kieve.ssss.repository.FontRepo.XIROD_32;
import static ca.kieve.ssss.ui.widget.GameWindow.TILE_SIZE;

public class GlyphRepo {
    private GlyphRepo() {
        // Do not instantiate
    }

    public static final String EXTRA_CHARS = "█";

    public static final TileGlyph PLAYER = create(
        XIROD_32,
        '@',
        2.5f / TILE_SIZE,
        1 + (-6f) / TILE_SIZE
    );

    public static final TileGlyph S = create(
        XIROD_32,
        'S',
        2f / TILE_SIZE,
        1 + (-6f) / TILE_SIZE
    );

    public static final TileGlyph POUND = create(
        XIROD_32,
        '#',
        7f / TILE_SIZE,
        1 + (-5f) / TILE_SIZE
    );

    public static final TileGlyph INTERPUNCT = create(
        XIROD_32,
        '·',
        13f / TILE_SIZE,
        1 + (-7.25f) / TILE_SIZE
    );

    public static final TileGlyph SOLID = create(
        UBUNTU_32,
        '█',
        7f / TILE_SIZE,
        1 + (-7f) / TILE_SIZE
    );

    private static TileGlyph create(BitmapFont font, char character, float dx, float dy) {
        return new TileGlyph(font, character, dx, dy);
    }
}
