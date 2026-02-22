package ca.kieve.ssss.editor;

import javafx.scene.paint.Color;

/**
 * Central source of truth for editor styling constants. Canvas/renderer
 * colors live here as static Color fields; JavaFX scene-graph styles are
 * driven by CSS class names whose constants are also defined here.
 */
public final class EditorTheme {
    // Canvas colors (used by GraphicsContext drawing, not CSS-styleable)
    public static final Color CANVAS_BACKGROUND = Color.gray(0.2);
    public static final Color CELL_BACKGROUND = Color.gray(0.1);
    public static final Color GRID_COLOR = Color.gray(0.85);
    public static final Color INFINITE_GRID_COLOR = Color.gray(0.28);

    // CSS style class names
    public static final String STYLE_TITLE = "editor-title";
    public static final String STYLE_SUBTITLE = "editor-subtitle";
    public static final String STYLE_COMPONENT_BOX = "editor-component-box";
    public static final String STYLE_COMPONENT_TYPE =
            "editor-component-type";
    public static final String STYLE_COMPONENT_MARKER =
            "editor-component-marker";
    public static final String STYLE_TOOLBAR = "editor-toolbar";
    public static final String STYLE_TOOLBAR_LABEL_BOLD =
            "editor-toolbar-label-bold";
    public static final String STYLE_TOOLBAR_LABEL =
            "editor-toolbar-label";
    public static final String STYLE_TITLE_BAR = "editor-title-bar";
    public static final String STYLE_HIDDEN_TAB_HEADER =
            "editor-hidden-tab-header";
    public static final String STYLE_TITLE_TAB = "title-tab";
    public static final String STYLE_WINDOW_BUTTON = "window-button";
    public static final String STYLE_WINDOW_BUTTON_CLOSE =
            "window-button-close";

    /** Path to the editor CSS stylesheet, relative to this class. */
    public static final String STYLESHEET =
            EditorTheme.class.getResource("editor.css")
                    .toExternalForm();

    private EditorTheme() {}
}
