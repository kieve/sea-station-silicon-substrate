package ca.kieve.ssss.context;

import ca.kieve.ssss.util.Vec3i;

public class ExamineContext {
    private RenderContext m_renderContext;
    private boolean m_active = false;
    private Vec3i m_crosshairPos = new Vec3i(0, 0, 0);
    private boolean m_selectionMode = false;
    private int m_selectedIndex = 0;

    public void init(GameContext gameContext) {
        m_renderContext = gameContext.render();
    }

    public boolean isActive() {
        return m_active;
    }

    public void enter(Vec3i startPos) {
        m_active = true;
        m_crosshairPos.set(startPos);
        m_selectionMode = false;
        m_selectedIndex = 0;
        markDirty();
    }

    public void exit() {
        m_active = false;
        m_selectionMode = false;
        m_selectedIndex = 0;
        markDirty();
    }

    public Vec3i getCrosshairPos() {
        return m_crosshairPos;
    }

    public void moveCrosshair(Vec3i delta) {
        m_crosshairPos.addMut(delta);
        markDirty();
    }

    private void markDirty() {
        if (m_renderContext != null) {
            m_renderContext.markDirty();
        }
    }

    public boolean isSelectionMode() {
        return m_selectionMode;
    }

    public void enterSelectionMode() {
        m_selectionMode = true;
        m_selectedIndex = 0;
    }

    public void exitSelectionMode() {
        m_selectionMode = false;
        m_selectedIndex = 0;
    }

    public int getSelectedIndex() {
        return m_selectedIndex;
    }

    public void setSelectedIndex(int index) {
        m_selectedIndex = index;
    }

    public void incrementSelectedIndex(int maxIndex) {
        m_selectedIndex = (m_selectedIndex + 1) % maxIndex;
    }

    public void decrementSelectedIndex(int maxIndex) {
        m_selectedIndex = (m_selectedIndex - 1 + maxIndex) % maxIndex;
    }
}
