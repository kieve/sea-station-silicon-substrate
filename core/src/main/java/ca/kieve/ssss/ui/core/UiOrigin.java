package ca.kieve.ssss.ui.core;

public class UiOrigin {
    private UiPosition m_origin;

    public UiOrigin() {
        m_origin = UiPosition.ZERO;
    }

    public UiPosition getPosition() {
        return m_origin;
    }

    public void setPosition(UiPosition origin) {
        m_origin = origin;
    }
}
