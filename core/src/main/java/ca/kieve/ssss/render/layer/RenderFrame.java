package ca.kieve.ssss.render.layer;

import ca.kieve.ssss.render.glyphs.CellGlyphComposer;

public record RenderFrame(int cameraZ, boolean fullVision, CellGlyphComposer composer) {
}
