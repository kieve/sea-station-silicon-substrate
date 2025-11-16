package ca.kieve.ssss.context;

public class RenderContext {
    private boolean m_dirty = true;

    public boolean isDirty() {
        return m_dirty;
    }

    public void markDirty() {
        m_dirty = true;
    }

    public void clearDirty() {
        m_dirty = false;
    }
}
