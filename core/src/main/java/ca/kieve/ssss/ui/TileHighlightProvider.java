package ca.kieve.ssss.ui;

import java.util.List;

public interface TileHighlightProvider {
    boolean isHighlightActive();
    List<TileHighlight> getHighlights();
}
