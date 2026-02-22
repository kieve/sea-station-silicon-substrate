package ca.kieve.ssss.editor.ui;

import java.util.EnumSet;
import java.util.Set;

import javafx.animation.AnimationTimer;
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
    private static final Color BACKGROUND_COLOR = Color.gray(0.2);

    private static final Set<KeyCode> PAN_KEYS = EnumSet.of(
            KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN,
            KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S);

    private final Canvas m_canvas = new Canvas();
    private final Set<KeyCode> m_heldKeys = EnumSet.noneOf(KeyCode.class);
    private final EventHandler<KeyEvent> m_sceneKeyPressFilter =
            this::handleKeyPressed;
    private final EventHandler<KeyEvent> m_sceneKeyReleaseFilter =
            this::handleKeyReleased;

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
        setStyle(
                "-fx-background-color: #" + toHex(BACKGROUND_COLOR) + ";");

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

    /**
     * Center the viewport on the given world-space point. If the canvas
     * has not yet been laid out, the centering is deferred until it has.
     */
    public void centerOn(double worldX, double worldY) {
        if (m_canvas.getWidth() > 0 && m_canvas.getHeight() > 0) {
            m_cameraX = worldX - m_canvas.getWidth() / 2.0;
            m_cameraY = worldY - m_canvas.getHeight() / 2.0;
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
            m_cameraX =
                    m_pendingCenterX - m_canvas.getWidth() / 2.0;
            m_cameraY =
                    m_pendingCenterY - m_canvas.getHeight() / 2.0;
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
            }
            requestFocus();
            e.consume();
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                m_cameraX = m_dragStartCameraX
                        - (e.getScreenX() - m_dragStartX);
                m_cameraY = m_dragStartCameraY
                        - (e.getScreenY() - m_dragStartY);
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
                    m_cameraX += dx;
                    m_cameraY += dy;
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
