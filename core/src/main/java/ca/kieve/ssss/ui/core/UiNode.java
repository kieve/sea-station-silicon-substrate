package ca.kieve.ssss.ui.core;

public abstract class UiNode {
    protected UiPosition m_position;
    protected UiSize m_size;
    protected UiPosition m_origin;

    public UiNode() {
        m_position = UiPosition.ZERO;
        m_size = UiSize.ZERO;
        m_origin = UiPosition.ZERO;
    }

    public UiPosition getPosition() {
        return m_position;
    }

    public void setPosition(UiPosition position) {
        m_position = position;
    }

    public UiSize getSize() {
        return m_size;
    }

    public void setSize(UiSize size) {
        m_size = size;
    }

    public UiPosition getOrigin() {
        return m_origin;
    }

    public void setOrigin(UiPosition origin) {
        m_origin = origin;
    }

    public abstract void update(UiRenderContext renderContext, float delta);
    public abstract void render(UiRenderContext renderContext, float delta);
}
