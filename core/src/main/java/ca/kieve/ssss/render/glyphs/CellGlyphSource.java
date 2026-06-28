package ca.kieve.ssss.render.glyphs;

public interface CellGlyphSource {
    void collect(int cameraZ, CellGlyphComposer out);
}
