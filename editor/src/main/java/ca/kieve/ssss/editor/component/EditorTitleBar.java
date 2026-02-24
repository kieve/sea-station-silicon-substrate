package ca.kieve.ssss.editor.component;

import static ca.kieve.ssss.editor.util.CssUtil.inline;

import java.util.function.BooleanSupplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;

import ca.kieve.ssss.editor.ui.AppIcon;
import ca.kieve.ssss.editor.ui.WindowsAeroSnap;

/**
 * Unified title bar combining menu, tab toggles, and window controls
 * in a single row, replacing the OS title bar + menu bar + tab header.
 */
public class EditorTitleBar extends HBox {
    private static final double HEIGHT = 32;

    private static final String STYLE_TITLE_BAR =
            "editor-title-bar";
    private static final String STYLE_TITLE_TAB = "title-tab";
    private static final String STYLE_WINDOW_BUTTON =
            "window-button";
    private static final String STYLE_WINDOW_BUTTON_CLOSE =
            "window-button-close";

    // language=css
    private static final String CSS = """
            .%1$s {
                -fx-pref-height: 32;
                -fx-min-height: 32;
                -fx-max-height: 32;
                -fx-background-color: -color-bg-subtle;
                -fx-alignment: center-left;
                -fx-padding: 0 0 0 4;
            }
            .%2$s {
                -fx-background-color: transparent;
                -fx-background-radius: 0;
                -fx-border-color: transparent transparent transparent transparent;
                -fx-border-width: 0 0 2 0;
                -fx-padding: 6 14;
                -fx-text-fill: -color-fg-muted;
                -fx-cursor: hand;
            }
            .%2$s:selected {
                -fx-border-color: transparent transparent -color-accent-fg transparent;
                -fx-text-fill: -color-fg-default;
            }
            .%2$s:hover {
                -fx-background-color: -color-neutral-muted;
            }
            .%3$s {
                -fx-background-color: transparent;
                -fx-background-radius: 0;
                -fx-pref-width: 46;
                -fx-pref-height: 32;
                -fx-min-width: 46;
                -fx-min-height: 32;
                -fx-padding: 0;
                -fx-text-fill: -color-fg-default;
                -fx-cursor: hand;
            }
            .%3$s:hover {
                -fx-background-color: -color-neutral-muted;
            }
            .%4$s:hover {
                -fx-background-color: #e81123;
                -fx-text-fill: white;
            }
            """.formatted(
            STYLE_TITLE_BAR,
            STYLE_TITLE_TAB,
            STYLE_WINDOW_BUTTON,
            STYLE_WINDOW_BUTTON_CLOSE);

    private static final String ICON_STROKE_STYLE =
            "-fx-stroke: -color-fg-default; -fx-stroke-width: 1;";

    private final Stage m_stage;
    private final TabPane m_tabPane;
    private final ToggleGroup m_toggleGroup;
    private final HBox m_tabBox;
    private final Button m_maxBtn;
    private final BooleanSupplier m_canCloseTab;

    private final BooleanProperty m_maximized =
            new SimpleBooleanProperty(false);

    private double m_dragOffsetX;
    private double m_dragOffsetY;
    private boolean m_dragging;

    // Saved bounds for restore-from-maximize
    private double m_restoreX;
    private double m_restoreY;
    private double m_restoreW;
    private double m_restoreH;

    public EditorTitleBar(
            Stage stage,
            TabPane tabPane,
            Runnable onLoadMap,
            Runnable onSave,
            Runnable onSaveAs,
            Runnable onClose,
            BooleanSupplier canCloseTab) {
        m_stage = stage;
        m_tabPane = tabPane;
        m_canCloseTab = canCloseTab;
        m_toggleGroup = new ToggleGroup();
        m_tabBox = new HBox();
        m_tabBox.setAlignment(Pos.CENTER_LEFT);
        m_tabBox.setSpacing(0);

        getStylesheets().add(inline(CSS));
        getStyleClass().add(STYLE_TITLE_BAR);
        setPrefHeight(HEIGHT);
        setMinHeight(HEIGHT);
        setMaxHeight(HEIGHT);
        setAlignment(Pos.CENTER_LEFT);

        // Left: app icon, file menu button, then tabs
        var appIcon = new ImageView(
                AppIcon.create(16));
        appIcon.setFitWidth(16);
        appIcon.setFitHeight(16);
        appIcon.setSmooth(true);
        HBox.setMargin(appIcon, new Insets(0, 4, 0, 0));

        var loadMapItem = new MenuItem("Load Map...");
        loadMapItem.setOnAction(e -> onLoadMap.run());

        var saveItem = new MenuItem("Save");
        saveItem.setOnAction(e -> onSave.run());

        var saveAsItem = new MenuItem("Save As...");
        saveAsItem.setOnAction(e -> onSaveAs.run());

        var fileMenu = new MenuButton("File", null,
                loadMapItem,
                new SeparatorMenuItem(),
                saveItem,
                saveAsItem);

        // Spacer pushes window buttons to the right
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right: window control buttons with Win11-style icons
        var minBtn = new Button();
        minBtn.setGraphic(createMinimizeIcon());
        minBtn.getStyleClass().add(STYLE_WINDOW_BUTTON);
        minBtn.setOnAction(e -> m_stage.setIconified(true));
        minBtn.setFocusTraversable(false);

        m_maxBtn = new Button();
        m_maxBtn.setGraphic(createMaximizeIcon());
        m_maxBtn.getStyleClass().add(STYLE_WINDOW_BUTTON);
        m_maxBtn.setOnAction(e -> toggleMaximize());
        m_maxBtn.setFocusTraversable(false);

        var closeBtn = new Button();
        closeBtn.setGraphic(createCloseIcon());
        closeBtn.getStyleClass().addAll(
                STYLE_WINDOW_BUTTON,
                STYLE_WINDOW_BUTTON_CLOSE);
        closeBtn.setOnAction(e -> onClose.run());
        closeBtn.setFocusTraversable(false);

        var windowButtons = new HBox(minBtn, m_maxBtn, closeBtn);
        windowButtons.setAlignment(Pos.CENTER_RIGHT);
        windowButtons.setSpacing(0);

        getChildren().addAll(
                appIcon, fileMenu, m_tabBox, spacer, windowButtons);

        // Sync initial tabs
        for (Tab tab : m_tabPane.getTabs()) {
            addTabToggle(tab);
        }
        syncSelection();

        // Listen for tab list changes
        m_tabPane.getTabs().addListener(
                (ListChangeListener<Tab>) change -> {
            while (change.next()) {
                for (Tab removed : change.getRemoved()) {
                    removeTabToggle(removed);
                }
                for (Tab added : change.getAddedSubList()) {
                    addTabToggle(added);
                }
            }
        });

        // Sync toggle → tab selection
        m_toggleGroup.selectedToggleProperty().addListener(
                (obs, oldVal, newVal) -> {
            if (newVal instanceof ToggleButton btn) {
                Tab tab = (Tab) btn.getUserData();
                m_tabPane.getSelectionModel().select(tab);
            }
        });

        // Sync tab selection → toggle
        m_tabPane.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> syncSelection());

        // Drag to move window — delegate to native Win32 drag
        // so that Aero Snap previews work. Falls back to manual
        // JavaFX drag on non-Windows platforms.
        setOnMousePressed(e -> {
            if (e.getTarget() != this
                    && !(e.getTarget() instanceof Region)) {
                return;
            }

            // Try native drag first (enables snap previews).
            // Dragging a maximized window restores it.
            if (WindowsAeroSnap.startNativeDrag()) {
                if (m_maximized.get()) {
                    m_maximized.set(false);
                    m_maxBtn.setGraphic(
                            createMaximizeIcon());
                }
                e.consume();
                return;
            }

            // Fallback: manual JavaFX drag
            m_dragging = true;
            m_dragOffsetX = e.getScreenX() - m_stage.getX();
            m_dragOffsetY = e.getScreenY() - m_stage.getY();
        });

        setOnMouseDragged(e -> {
            if (!m_dragging) {
                return;
            }
            m_stage.setX(e.getScreenX() - m_dragOffsetX);
            m_stage.setY(e.getScreenY() - m_dragOffsetY);
        });

        setOnMouseReleased(e -> m_dragging = false);

        // Double-click to maximize/restore
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                toggleMaximize();
            }
        });
    }

    public BooleanProperty maximizedProperty() {
        return m_maximized;
    }

    private void toggleMaximize() {
        if (m_maximized.get()) {
            m_maximized.set(false);
            m_stage.setX(m_restoreX);
            m_stage.setY(m_restoreY);
            m_stage.setWidth(m_restoreW);
            m_stage.setHeight(m_restoreH);
            m_maxBtn.setGraphic(createMaximizeIcon());
        } else {
            m_restoreX = m_stage.getX();
            m_restoreY = m_stage.getY();
            m_restoreW = m_stage.getWidth();
            m_restoreH = m_stage.getHeight();

            Rectangle2D bounds = Screen.getScreensForRectangle(
                    m_stage.getX(), m_stage.getY(),
                    m_stage.getWidth(), m_stage.getHeight())
                    .getFirst()
                    .getVisualBounds();
            m_maximized.set(true);
            m_stage.setX(bounds.getMinX());
            m_stage.setY(bounds.getMinY());
            m_stage.setWidth(bounds.getWidth());
            m_stage.setHeight(bounds.getHeight());
            m_maxBtn.setGraphic(createRestoreIcon());
        }
    }

    private void addTabToggle(Tab tab) {
        var btn = new ToggleButton(tab.getText());
        btn.setToggleGroup(m_toggleGroup);
        btn.setUserData(tab);
        btn.getStyleClass().add(STYLE_TITLE_TAB);
        btn.setFocusTraversable(false);

        // Keep button text in sync with tab text
        tab.textProperty().addListener(
                (obs, oldVal, newVal) -> btn.setText(newVal));

        // Add close button for closable tabs
        if (tab.isClosable()) {
            var closeLabel = new Label(" \u00D7");
            closeLabel.setStyle("-fx-text-fill: -color-fg-muted;");
            closeLabel.setOnMouseClicked(e -> {
                if (!m_canCloseTab.getAsBoolean()) {
                    e.consume();
                    return;
                }
                m_tabPane.getTabs().remove(tab);
                if (tab.getOnClosed() != null) {
                    tab.getOnClosed().handle(null);
                }
                e.consume();
            });
            btn.setGraphic(closeLabel);
            btn.setContentDisplay(
                    javafx.scene.control.ContentDisplay.RIGHT);
        }

        m_tabBox.getChildren().add(btn);
    }

    private void removeTabToggle(Tab tab) {
        m_tabBox.getChildren().removeIf(node -> {
            if (node instanceof ToggleButton btn) {
                return btn.getUserData() == tab;
            }
            return false;
        });
    }

    private void syncSelection() {
        Tab selected = m_tabPane.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        for (var node : m_tabBox.getChildren()) {
            if (node instanceof ToggleButton btn
                    && btn.getUserData() == selected) {
                btn.setSelected(true);
                break;
            }
        }
    }

    // Win11-style window chrome icons drawn with shapes

    private static javafx.scene.Node createMinimizeIcon() {
        var line = new Line(0, 0, 10, 0);
        line.setStyle(ICON_STROKE_STYLE);
        return line;
    }

    private static javafx.scene.Node createMaximizeIcon() {
        var rect = new Rectangle(10, 10);
        rect.setFill(null);
        rect.setStyle(ICON_STROKE_STYLE);
        return rect;
    }

    private static javafx.scene.Node createRestoreIcon() {
        // Two overlapping rectangles like Win11 restore icon
        var back = new Rectangle(3, 0, 8, 8);
        back.setFill(null);
        back.setStyle(ICON_STROKE_STYLE);

        var front = new Rectangle(0, 3, 8, 8);
        front.setFill(null);
        front.setStyle(ICON_STROKE_STYLE
                + "-fx-fill: -color-bg-subtle;");

        var group = new javafx.scene.Group(back, front);
        return group;
    }

    private static javafx.scene.Node createCloseIcon() {
        var line1 = new Line(0, 0, 10, 10);
        line1.setStyle(ICON_STROKE_STYLE);
        var line2 = new Line(10, 0, 0, 10);
        line2.setStyle(ICON_STROKE_STYLE);
        return new javafx.scene.Group(line1, line2);
    }
}
