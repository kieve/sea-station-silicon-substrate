package ca.kieve.ssss.repository;

import ca.kieve.ssss.component.TileGlyph;

import static ca.kieve.ssss.MainEngine.TILE_SIZE;
import static ca.kieve.ssss.repository.FontRepo.XIROD_32;

public class GlyphRepo {
    private GlyphRepo() {
        // Do not instantiate
    }

    public static final TileGlyph PLAYER = create(
        '@',
        2.5f / TILE_SIZE,
        1 + (-4.4f) / TILE_SIZE
    );

    public static final TileGlyph POUND = create(
        '#',
        7f / TILE_SIZE,
        1 + (-5f) / TILE_SIZE
    );

    public static final TileGlyph INTERPUNCT = create(
        '·',
        13f / TILE_SIZE,
        1 + (-7.25f) / TILE_SIZE
    );

    public static final TileGlyph SOLID = create('█', 0, 0);

    private static TileGlyph create(char character, float dx, float dy) {
        return new TileGlyph(character, XIROD_32, dx, dy);
    }
}
