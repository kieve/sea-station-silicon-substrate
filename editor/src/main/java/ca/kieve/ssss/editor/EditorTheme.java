package ca.kieve.ssss.editor;

import javafx.scene.paint.Color;

/**
 * Canvas/renderer colors and shared CSS constants.
 */
public final class EditorTheme {
    public static final Color CANVAS_BACKGROUND = Color.gray(0.2);
    public static final Color CELL_BACKGROUND = Color.gray(0.1);
    public static final Color GRID_COLOR = Color.gray(0.85);
    public static final Color INFINITE_GRID_COLOR = Color.gray(0.28);
    public static final Color ENTITY_MARKER_COLOR = Color.LIMEGREEN;
    public static final Color SELECTION_COLOR = Color.CYAN;
    public static final Color SUBMAP_GHOST_COLOR = Color.web("#ff9b3a");
    public static final Color CONNECTOR_COLOR = Color.web("#ffd24a");
    public static final Color OVERLAP_WARNING_COLOR = Color.web("#ff4040");

    public static final String STYLE_OVERLAY = "editor-overlay";

    // language=css
    public static final String OVERLAY_CSS = """
        .%s {
            -fx-background-color: rgba(30, 30, 30, 0.85);
            -fx-background-radius: 6;
            -fx-padding: 4 8;
            -fx-font-size: 11;
        }
        """.formatted(STYLE_OVERLAY);

    private EditorTheme() {
    }
}
