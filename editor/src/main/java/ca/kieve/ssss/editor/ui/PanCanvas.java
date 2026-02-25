package ca.kieve.ssss.editor.ui;

import ca.kieve.ssss.editor.EditorTheme;

import java.util.EnumSet;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * A reusable pannable canvas viewport. Contains an auto-sizing Canvas with
 * clipping, middle-click drag panning, and WASD/arrow key panning.
 */
public class PanCanvas extends Pane {
    private static final double PAN_SPEED = 360.0;

    private static final Set<KeyCode> PAN_KEYS = EnumSet.of(
            KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN,
            KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S);

    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 4.0;
    private static final double ZOOM_STEP = 1.1;

    private final Canvas m_canvas = new Canvas();
    private final Set<KeyCode> m_heldKeys = EnumSet.noneOf(KeyCode.class);
    private final EventHandler<KeyEvent> m_sceneKeyPressFilter =
            this::handleKeyPressed;
    private final EventHandler<KeyEvent> m_sceneKeyReleaseFilter =
            this::handleKeyReleased;
    private final DoubleProperty m_zoom =
            new SimpleDoubleProperty(1.0);

    private double m_cameraX;
    private double m_cameraY;
    private boolean m_needsCenter;
    private double m_pendingCenterX;
    private double m_pendingCenterY;
    private Runnable m_onRedraw;

    // Middle-click drag state
    private double m_dragStartX;
    private double m_dragStartY;
    private double m_dragStartCameraX;
    private double m_dragStartCameraY;

    public PanCanvas() {
        getChildren().add(m_canvas);
        setStyle("-fx-background-color: #"
                + toHex(EditorTheme.CANVAS_BACKGROUND) + ";");

        var clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        widthProperty().addListener((obs, oldVal, newVal) -> {
            m_canvas.setWidth(newVal.doubleValue());
            onViewportResized();
        });
        heightProperty().addListener((obs, oldVal, newVal) -> {
            m_canvas.setHeight(newVal.doubleValue());
            onViewportResized();
        });

        setupPanning();
    }

    public Canvas getCanvas() {
        return m_canvas;
    }

    public double getCameraX() {
        return m_cameraX;
    }

    public double getCameraY() {
        return m_cameraY;
    }

    public void setOnRedraw(Runnable onRedraw) {
        m_onRedraw = onRedraw;
    }

    public double getZoom() {
        return m_zoom.get();
    }

    public DoubleProperty zoomProperty() {
        return m_zoom;
    }

    /**
     * Zoom toward a screen-space anchor point (e.g. the mouse
     * position). The world point under the anchor stays fixed.
     */
    public void zoom(int direction, double anchorX, double anchorY) {
        double oldZoom = m_zoom.get();
        double factor = direction > 0 ? ZOOM_STEP : 1.0 / ZOOM_STEP;
        double newZoom = Math.clamp(
                oldZoom * factor, MIN_ZOOM, MAX_ZOOM);
        if (newZoom == oldZoom) {
            return;
        }

        // World point under the anchor before zoom
        double worldX = m_cameraX + anchorX / oldZoom;
        double worldY = m_cameraY + anchorY / oldZoom;

        m_zoom.set(newZoom);

        // Adjust camera so the same world point stays under
        // the anchor
        m_cameraX = worldX - anchorX / newZoom;
        m_cameraY = worldY - anchorY / newZoom;
        requestRedraw();
    }

    public void resetZoom() {
        double oldZoom = m_zoom.get();
        if (oldZoom == 1.0) {
            return;
        }

        // Keep center of viewport fixed
        double centerX = m_canvas.getWidth() / 2.0;
        double centerY = m_canvas.getHeight() / 2.0;
        double worldX = m_cameraX + centerX / oldZoom;
        double worldY = m_cameraY + centerY / oldZoom;

        m_zoom.set(1.0);
        m_cameraX = worldX - centerX;
        m_cameraY = worldY - centerY;
        requestRedraw();
    }

    /**
     * Center the viewport on the given world-space point. If the canvas
     * has not yet been laid out, the centering is deferred until it has.
     */
    public void centerOn(double worldX, double worldY) {
        double zoom = m_zoom.get();
        if (m_canvas.getWidth() > 0 && m_canvas.getHeight() > 0) {
            m_cameraX =
                    worldX - m_canvas.getWidth() / (2.0 * zoom);
            m_cameraY =
                    worldY - m_canvas.getHeight() / (2.0 * zoom);
        } else {
            m_needsCenter = true;
            m_pendingCenterX = worldX;
            m_pendingCenterY = worldY;
        }
    }

    public void requestRedraw() {
        if (m_onRedraw != null) {
            m_onRedraw.run();
        }
    }

    private void onViewportResized() {
        if (m_needsCenter
                && m_canvas.getWidth() > 0
                && m_canvas.getHeight() > 0) {
            m_needsCenter = false;
            double zoom = m_zoom.get();
            m_cameraX = m_pendingCenterX
                    - m_canvas.getWidth() / (2.0 * zoom);
            m_cameraY = m_pendingCenterY
                    - m_canvas.getHeight() / (2.0 * zoom);
        }
        requestRedraw();
    }

    private void setupPanning() {
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                m_dragStartX = e.getScreenX();
                m_dragStartY = e.getScreenY();
                m_dragStartCameraX = m_cameraX;
                m_dragStartCameraY = m_cameraY;
                e.consume();
            }
            requestFocus();
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                double zoom = m_zoom.get();
                m_cameraX = m_dragStartCameraX
                        - (e.getScreenX() - m_dragStartX) / zoom;
                m_cameraY = m_dragStartCameraY
                        - (e.getScreenY() - m_dragStartY) / zoom;
                requestRedraw();
            }
        });

        setFocusTraversable(true);

        // Register scene-level event filters so we intercept
        // arrow keys before TabPane's skin can use them for tab
        // switching. Only active when this canvas has focus.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(
                        KeyEvent.KEY_PRESSED, m_sceneKeyPressFilter);
                oldScene.removeEventFilter(
                        KeyEvent.KEY_RELEASED,
                        m_sceneKeyReleaseFilter);
            }
            if (newScene != null) {
                newScene.addEventFilter(
                        KeyEvent.KEY_PRESSED, m_sceneKeyPressFilter);
                newScene.addEventFilter(
                        KeyEvent.KEY_RELEASED,
                        m_sceneKeyReleaseFilter);
            }
        });

        // Clear held keys when focus is lost
        focusedProperty().addListener(
                (obs, wasFocused, nowFocused) -> {
            if (!nowFocused) {
                m_heldKeys.clear();
            }
        });

        new AnimationTimer() {
            private long m_lastNanos;

            @Override
            public void handle(long now) {
                if (m_heldKeys.isEmpty()) {
                    m_lastNanos = 0;
                    return;
                }
                if (m_lastNanos == 0) {
                    m_lastNanos = now;
                    return;
                }
                double dt = (now - m_lastNanos) / 1_000_000_000.0;
                m_lastNanos = now;

                double dx = 0;
                double dy = 0;
                if (m_heldKeys.contains(KeyCode.LEFT)
                        || m_heldKeys.contains(KeyCode.A)) {
                    dx -= PAN_SPEED * dt;
                }
                if (m_heldKeys.contains(KeyCode.RIGHT)
                        || m_heldKeys.contains(KeyCode.D)) {
                    dx += PAN_SPEED * dt;
                }
                if (m_heldKeys.contains(KeyCode.UP)
                        || m_heldKeys.contains(KeyCode.W)) {
                    dy -= PAN_SPEED * dt;
                }
                if (m_heldKeys.contains(KeyCode.DOWN)
                        || m_heldKeys.contains(KeyCode.S)) {
                    dy += PAN_SPEED * dt;
                }
                if (dx != 0 || dy != 0) {
                    double zoom = m_zoom.get();
                    m_cameraX += dx / zoom;
                    m_cameraY += dy / zoom;
                    requestRedraw();
                }
            }
        }.start();
    }

    private void handleKeyPressed(KeyEvent e) {
        if (!isFocused()) {
            return;
        }
        if (PAN_KEYS.contains(e.getCode())) {
            m_heldKeys.add(e.getCode());
            e.consume();
        }
    }

    private void handleKeyReleased(KeyEvent e) {
        if (PAN_KEYS.contains(e.getCode())) {
            m_heldKeys.remove(e.getCode());
            e.consume();
        }
    }

    private static String toHex(Color color) {
        return String.format("%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
