package ca.kieve.ssss.editor.ui;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Handles mouse-driven resize on undecorated window edges and corners.
 * Attach to a scene via {@link #install(Scene, Stage)}.
 */
public final class WindowResizeHandler {
    private static final double BORDER = 5;
    private static final double MIN_WIDTH = 400;
    private static final double MIN_HEIGHT = 300;

    private final Stage m_stage;

    private boolean m_resizing;
    private boolean m_resizeN;
    private boolean m_resizeS;
    private boolean m_resizeE;
    private boolean m_resizeW;

    private double m_startX;
    private double m_startY;
    private double m_startStageX;
    private double m_startStageY;
    private double m_startStageW;
    private double m_startStageH;

    private WindowResizeHandler(Stage stage) {
        m_stage = stage;
    }

    public static void install(Scene scene, Stage stage) {
        var handler = new WindowResizeHandler(stage);
        scene.addEventFilter(
                MouseEvent.MOUSE_MOVED, handler::onMouseMoved);
        scene.addEventFilter(
                MouseEvent.MOUSE_PRESSED, handler::onMousePressed);
        scene.addEventFilter(
                MouseEvent.MOUSE_DRAGGED, handler::onMouseDragged);
        scene.addEventFilter(
                MouseEvent.MOUSE_RELEASED, handler::onMouseReleased);
    }

    private void onMouseMoved(MouseEvent e) {
        if (m_stage.isMaximized()) {
            m_stage.getScene().setCursor(Cursor.DEFAULT);
            return;
        }
        m_stage.getScene().setCursor(cursorForPosition(e));
    }

    private void onMousePressed(MouseEvent e) {
        if (m_stage.isMaximized()) {
            return;
        }
        Cursor cursor = cursorForPosition(e);
        if (cursor == Cursor.DEFAULT) {
            return;
        }

        m_resizing = true;
        m_resizeN = cursor == Cursor.N_RESIZE
                || cursor == Cursor.NE_RESIZE
                || cursor == Cursor.NW_RESIZE;
        m_resizeS = cursor == Cursor.S_RESIZE
                || cursor == Cursor.SE_RESIZE
                || cursor == Cursor.SW_RESIZE;
        m_resizeE = cursor == Cursor.E_RESIZE
                || cursor == Cursor.NE_RESIZE
                || cursor == Cursor.SE_RESIZE;
        m_resizeW = cursor == Cursor.W_RESIZE
                || cursor == Cursor.NW_RESIZE
                || cursor == Cursor.SW_RESIZE;

        m_startX = e.getScreenX();
        m_startY = e.getScreenY();
        m_startStageX = m_stage.getX();
        m_startStageY = m_stage.getY();
        m_startStageW = m_stage.getWidth();
        m_startStageH = m_stage.getHeight();
        e.consume();
    }

    private void onMouseDragged(MouseEvent e) {
        if (!m_resizing) {
            return;
        }

        double dx = e.getScreenX() - m_startX;
        double dy = e.getScreenY() - m_startY;

        if (m_resizeE) {
            double newW = Math.max(MIN_WIDTH, m_startStageW + dx);
            m_stage.setWidth(newW);
        }
        if (m_resizeS) {
            double newH = Math.max(MIN_HEIGHT, m_startStageH + dy);
            m_stage.setHeight(newH);
        }
        if (m_resizeW) {
            double newW = Math.max(MIN_WIDTH, m_startStageW - dx);
            if (newW > MIN_WIDTH) {
                m_stage.setX(m_startStageX + dx);
            }
            m_stage.setWidth(newW);
        }
        if (m_resizeN) {
            double newH = Math.max(MIN_HEIGHT, m_startStageH - dy);
            if (newH > MIN_HEIGHT) {
                m_stage.setY(m_startStageY + dy);
            }
            m_stage.setHeight(newH);
        }

        e.consume();
    }

    private void onMouseReleased(MouseEvent e) {
        if (m_resizing) {
            m_resizing = false;
            e.consume();
        }
    }

    private Cursor cursorForPosition(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        double w = m_stage.getScene().getWidth();
        double h = m_stage.getScene().getHeight();

        boolean top = y < BORDER;
        boolean bottom = y > h - BORDER;
        boolean left = x < BORDER;
        boolean right = x > w - BORDER;

        if (top && left) return Cursor.NW_RESIZE;
        if (top && right) return Cursor.NE_RESIZE;
        if (bottom && left) return Cursor.SW_RESIZE;
        if (bottom && right) return Cursor.SE_RESIZE;
        if (top) return Cursor.N_RESIZE;
        if (bottom) return Cursor.S_RESIZE;
        if (left) return Cursor.W_RESIZE;
        if (right) return Cursor.E_RESIZE;

        return Cursor.DEFAULT;
    }
}
